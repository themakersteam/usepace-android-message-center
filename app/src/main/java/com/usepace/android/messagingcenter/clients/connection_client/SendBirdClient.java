package com.usepace.android.messagingcenter.clients.connection_client;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import com.google.firebase.messaging.RemoteMessage;
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
import com.usepace.android.messagingcenter.network.sendbird.SendBirdPlatformApi;
import com.usepace.android.messagingcenter.network.sendbird.SendBirdPlatformApiCallbackInterface;
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
            public void onConnected(User user,final SendBirdException e) {
                SendBirdPlatformApi.Instance().login(connectionRequest, new SendBirdPlatformApiCallbackInterface<String>() {
                    @Override
                    public void onSuccess(String result) {
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
                                            //Disconnecting for receiving push notifications
                                            SendBird.disconnect(new SendBird.DisconnectHandler() {
                                                @Override
                                                public void onDisconnected() {

                                                }
                                            });
                                            connectionInterface.onMessageCenterConnected();
                                        }
                                    }
                                });
                            }
                        }
                    }
                    @Override
                    public void onError(String error) {
                        if (connectionInterface != null) {
                            connectionInterface.onMessageCenterConnectionError(102, new MessageCenterException(error));
                        }
                    }
                });
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
        SendBirdPlatformApi.Instance().getTotalUnReadMessageCount(lastConnecitonRequest, chat_id, new SendBirdPlatformApiCallbackInterface<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                unReadMessagesInterface.onUnreadMessages(result);
            }

            @Override
            public void onError(String error) {
                unReadMessagesInterface.onErrorRetrievingMessages(new MessageCenterException(error));
            }
        });
    }

    @Override
    public void openChatView(final Activity context, final String chat_id, final Theme theme, final OpenChatViewInterface openChatViewInterface) {
        if (!isConnected() && lastConnecitonRequest != null) {
            connect(context, lastConnecitonRequest, new ConnectionInterface() {
                @Override
                public void onMessageCenterConnected() {
                    openChatView(context, theme, chat_id, openChatViewInterface);
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
            openChatView(context, theme, chat_id, openChatViewInterface);
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
    public void disconnect(Context context, final DisconnectInterface disconnectInterface) {
        lastConnecitonRequest = null;
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        try {
            SendBird.unregisterPushTokenAllForCurrentUser(new SendBird.UnregisterPushTokenHandler() {
                @Override
                public void onUnregistered(SendBirdException e) {
                    disconnectApp(disconnectInterface);
                }
            });
        }
        catch (Exception e) { // SendBird instance hasn't been initialized.
            disconnectInterface.onMessageCenterDisconnected();
        }
    }

    @Override
    public void sdkHandleNotification(Context context, Class next, int icon, String title, RemoteMessage remoteMessage, List<String> messages, SdkHandleNotificationInterface sdkHandleNotificationInterface) {
        if (remoteMessage.getData().containsKey("sendbird")) {
            try {
                JSONObject jsonObject = new JSONObject(remoteMessage.getData().get("sendbird"));
                String message = remoteMessage.getData().get("message");
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

    private void openChatView(Activity activity, Theme theme, String chat_id, OpenChatViewInterface openChatViewInterface) {
        if (openChatViewInterface != null) {
            openChatViewInterface.onViewWillStart();
        }
        Intent a1 = new Intent(activity, SendBirdChatActivity.class);
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
        a1.putExtra("PACKAGE_NAME", activity.getPackageName());
        activity.startActivityForResult(a1, MessageCenter.OPEN_CHAT_VIEW_REQUEST_CODE);
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

    /**
     * @return an instance of the client
     */
    public static SendBirdClient Instance() {
        if (sendbirdClient == null)
            sendbirdClient = new SendBirdClient();
        return sendbirdClient;
    }
}
