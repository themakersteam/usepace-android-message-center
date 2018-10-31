package com.usepace.android.messagingcenter.clients.connection_client;

import android.content.Context;
import android.content.Intent;
import com.google.firebase.messaging.RemoteMessage;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.screens.sendbird.SendBirdChatActivity;
import com.usepace.android.messagingcenter.utils.NotificationUtil;
import org.json.JSONObject;
import java.util.List;

class SendBirdClient extends ClientInterface {

    private static SendBirdClient sendbirdClient;

    @Override
    public void connect(Context context, final ConnectionRequest connectionRequest, final ConnectionInterface connectionInterface) {
        SendBird.init(connectionRequest.getAppId(), context);
        SendBird.connect(connectionRequest.getUserId() != null ? connectionRequest.getUserId() : "", connectionRequest.getAccessToken(), new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                if (connectionInterface != null) {
                    if (e != null) {
                        connectionInterface.onMessageCenterConnectionError(e.getCode(), e.getMessage());
                    } else {
                        if (connectionRequest.getFcmToken() == null) return;
                        SendBird.registerPushTokenForCurrentUser(connectionRequest.getFcmToken(),
                                new SendBird.RegisterPushTokenWithStatusHandler() {
                                    @Override
                                    public void onRegistered(SendBird.PushTokenRegistrationStatus status, SendBirdException e) {
                                        if (e != null) {    // Error.
                                            connectionInterface.onMessageCenterConnectionError(e.getCode(), e.getMessage());
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
    public void join(Context context, final String chat_id) {
        context.startActivity(new Intent(context, SendBirdChatActivity.class).putExtra("CHANNEL_URL", chat_id));
    }

    @Override
    public void disconnect(final DisconnectInterface disconnectInterface) {
        SendBird.disconnect(new SendBird.DisconnectHandler() {
            @Override
            public void onDisconnected() {
                if (disconnectInterface != null)
                    disconnectInterface.onMessageCenterDisconnected();
            }
        });
    }

    @Override
    public void handleNotification(Context context, int icon, String title, RemoteMessage remoteMessage, List<String> messages) {
        if (remoteMessage.getData().containsKey("sendbird")) {
            try {
                JSONObject jsonObject = new JSONObject(remoteMessage.getData().get("sendbird"));
                String message = jsonObject.getString("message");
                messages.add(message);
                Intent pendingIntent = new Intent(context, SendBirdChatActivity.class);
                pendingIntent.putExtra("CHANNEL_URL", jsonObject.getJSONObject("channel").getString("channel_url"));
                new NotificationUtil().generateOne(context, pendingIntent, icon, title, message, messages);
            }
            catch (Exception e){
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
