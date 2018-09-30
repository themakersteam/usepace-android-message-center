package com.usepace.android.messagingcenter.clients.connection_client;

import android.content.Context;
import android.content.Intent;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.screens.sendbird.SendBirdChatActivity;

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
                        connectionInterface.onMessageCenterConnected();
                        if (connectionRequest.getFcmToken() == null) return;
                        SendBird.registerPushTokenForCurrentUser(connectionRequest.getFcmToken(),
                                new SendBird.RegisterPushTokenWithStatusHandler() {
                                    @Override
                                    public void onRegistered(SendBird.PushTokenRegistrationStatus status, SendBirdException e) {
                                        if (e != null) {    // Error.
                                            return;
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

    /**
     * @return an instance of the client
     */
    public static SendBirdClient Instance() {
        if (sendbirdClient == null)
            sendbirdClient = new SendBirdClient();
        return sendbirdClient;
    }
}
