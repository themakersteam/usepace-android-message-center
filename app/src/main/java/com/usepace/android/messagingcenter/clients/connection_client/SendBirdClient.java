package com.usepace.android.messagingcenter.clients.connection_client;

import android.content.Context;
import android.content.Intent;
import com.google.firebase.messaging.RemoteMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.usepace.android.messagingcenter.exceptions.MessageCenterException;
import com.usepace.android.messagingcenter.interfaces.AppHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.CloseChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.interfaces.OpenChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.SdkHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.UnReadMessagesInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.model.Theme;
import com.usepace.android.messagingcenter.screens.sendbird.SendBirdChatActivity;
import com.usepace.android.messagingcenter.utils.NotificationUtil;
import org.json.JSONObject;
import java.util.List;

class SendBirdClient extends ClientInterface {

    private ConnectionRequest lastConnecitonRequest;
    private static SendBirdClient sendbirdClient;

    @Override
    public void connect(Context context, final ConnectionRequest connectionRequest, final ConnectionInterface connectionInterface) {
        this.lastConnecitonRequest = connectionRequest;
        SendBird.init(connectionRequest.getAppId(), context);
        SendBird.connect(connectionRequest.getUserId() != null ? connectionRequest.getUserId() : "", connectionRequest.getAccessToken(), new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                if (connectionInterface != null) {
                    if (e != null) {
                        connectionInterface.onMessageCenterConnectionError(e.getCode(), new MessageCenterException(e.getMessage()));
                    } else {
                        if (connectionRequest.getFcmToken() == null) return;
                        SendBird.registerPushTokenForCurrentUser(connectionRequest.getFcmToken(),
                                new SendBird.RegisterPushTokenWithStatusHandler() {
                                    @Override
                                    public void onRegistered(SendBird.PushTokenRegistrationStatus status, SendBirdException e) {
                                        if (e != null) {    // Error.
                                            connectionInterface.onMessageCenterConnectionError(e.getCode(), new MessageCenterException(e.getMessage()));
                                        }
                                        else {
                                            connectionInterface.onMessageCenterConnected();
                                        }
                                    }
                                });
                    }
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        if (SendBird.getConnectionState() != null) {
            return SendBird.getConnectionState().equals(SendBird.ConnectionState.OPEN);
        }
        return false;
    }

    @Override
    public void getUnReadMessagesCount(Context context, final String chat_id, final UnReadMessagesInterface unReadMessagesInterface) {
        if (unReadMessagesInterface == null)
            return;
        if (!isConnected() && lastConnecitonRequest != null) {
            connect(context, lastConnecitonRequest, new ConnectionInterface() {
                @Override
                public void onMessageCenterConnected() {
                    getUnReadMessagesCount(chat_id, unReadMessagesInterface);
                }

                @Override
                public void onMessageCenterConnectionError(int code, MessageCenterException e) {
                    unReadMessagesInterface.onErrorRetrievingMessages(e);
                }
            });
        }
        else if (isConnected()) {
            getUnReadMessagesCount(chat_id, unReadMessagesInterface);
        }
        else {
            unReadMessagesInterface.onErrorRetrievingMessages(new MessageCenterException("You have to be connected to be able to join Chat view !", 302));
        }
    }

    @Override
    public void openChatView(final Context context, final String chat_id, final Theme theme, final OpenChatViewInterface openChatViewInterface) {
        if (!isConnected() && lastConnecitonRequest != null) {
            connect(context, lastConnecitonRequest, new ConnectionInterface() {
                @Override
                public void onMessageCenterConnected() {
                    openChatView(context, theme, chat_id);
                }
                @Override
                public void onMessageCenterConnectionError(int code, MessageCenterException e) {
                    if (openChatViewInterface != null) {
                        openChatViewInterface.onError(e);
                    }
                }
            });
        }
        else if (isConnected()) {
            openChatView(context, theme, chat_id);
        }
        else {
            if (openChatViewInterface != null) {
                openChatViewInterface.onError(new MessageCenterException("You have to be connected to be able to join Chat view !", 302));
            }
        }
    }

    @Override
    public void closeChatView(Context context, CloseChatViewInterface closeChatViewInterface) {
        Intent i = new Intent(context, SendBirdChatActivity.class);
        i.putExtra("close",true);
        context.startActivity(i);
        if (closeChatViewInterface != null) {
            closeChatViewInterface.onClosed();
        }
    }

    @Override
    public void disconnect(final DisconnectInterface disconnectInterface) {
        lastConnecitonRequest = null;
        SendBird.unregisterPushTokenAllForCurrentUser(new SendBird.UnregisterPushTokenHandler() {
            @Override
            public void onUnregistered(SendBirdException e) {
                disconnectApp(disconnectInterface);
            }
        });
    }

    @Override
    public void sdkHandleNotification(Context context, Class next, int icon, String title, RemoteMessage remoteMessage, List<String> messages, SdkHandleNotificationInterface sdkHandleNotificationInterface) {
        if (remoteMessage.getData().containsKey("sendbird")) {
            try {
                JSONObject jsonObject = new JSONObject(remoteMessage.getData().get("sendbird"));
                String message = jsonObject.getString("message");
                messages.add(message);
                Intent pendingIntent = new Intent(context, next);
                pendingIntent.putExtra("CHANNEL_URL", jsonObject.getJSONObject("channel").getString("channel_url"));
                pendingIntent.putExtra("FROM_NOTIFICATION", true);
                if (sdkHandleNotificationInterface != null) {
                    sdkHandleNotificationInterface.onMatched(jsonObject.getJSONObject("channel").getString("channel_url"));
                }
                new NotificationUtil().generateOne(context, pendingIntent, icon, title, message, messages);
            }
            catch (Exception e){
            }
        }
    }

    @Override
    public void appHandleNotification(RemoteMessage remoteMessage, AppHandleNotificationInterface appHandleNotificationInterface) {
        try {
            if (appHandleNotificationInterface == null)
                return;
            if (remoteMessage.getData().containsKey("sendbird")) {
                appHandleNotificationInterface.onMatched(new JSONObject(remoteMessage.getData().get("sendbird")));
            }
            else{
                appHandleNotificationInterface.onUnMatched();
            }
        }
        catch (Exception e) {
            if (appHandleNotificationInterface != null) {
                appHandleNotificationInterface.onUnMatched();
            }
        }
    }

    private void openChatView(Context context, Theme theme, String chat_id) {
        Intent a1 = new Intent(context, SendBirdChatActivity.class);
        if (theme != null) {
            if (theme.getToolbarTitle() != null) {
                a1.putExtra("TITLE", theme.getToolbarTitle());
            }
            if (theme.getToolbarSubtitle() != null) {
                a1.putExtra("SUBTITLE", theme.getToolbarSubtitle());
            }
            if (theme.getWelcomeMessage() != null) {
                a1.putExtra("WELCOME_MESSAGE", theme.getWelcomeMessage());
            }
        }
        a1.putExtra("CHANNEL_URL", chat_id);
        context.startActivity(a1);
    }

    private void disconnectApp(final DisconnectInterface disconnectInterface) {
        if (isConnected()) {
            SendBird.disconnect(new SendBird.DisconnectHandler() {
                @Override
                public void onDisconnected() {
                    if (disconnectInterface != null)
                        disconnectInterface.onMessageCenterDisconnected();
                }
            });
        }
        else {
            if (disconnectInterface != null) {
                disconnectInterface.onMessageCenterDisconnected();
            }
        }
    }

    private void getUnReadMessagesCount(String chat_id,final UnReadMessagesInterface unReadMessagesInterface) {
        if (chat_id == null) {
            SendBird.getTotalUnreadMessageCount(new GroupChannel.GroupChannelTotalUnreadMessageCountHandler() {
                @Override
                public void onResult(int i, SendBirdException e) {
                    if (e != null) {
                        unReadMessagesInterface.onErrorRetrievingMessages(new MessageCenterException(e.getMessage()));
                    }
                    else {
                        unReadMessagesInterface.onUnreadMessages(i);
                    }
                }
            });
        }
        else {
            GroupChannel.getChannel(chat_id, new GroupChannel.GroupChannelGetHandler() {
                @Override
                public void onResult(GroupChannel groupChannel, SendBirdException e) {
                    if (e != null) {
                        unReadMessagesInterface.onErrorRetrievingMessages(new MessageCenterException(e.getMessage()));
                    }
                    else {
                        unReadMessagesInterface.onUnreadMessages(groupChannel.getUnreadMessageCount());
                    }
                }
            });
        }
    }

    /**
     * @return an instance of the client
     */
    public static SendBirdClient Instance() {
        if (sendbirdClient == null)
            sendbirdClient = new SendBirdClient();
        return sendbirdClient;
    }
}
