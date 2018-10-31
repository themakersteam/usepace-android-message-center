package com.usepace.android.messagingcenter.clients.connection_client;

import android.content.Context;
import com.google.firebase.messaging.RemoteMessage;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import java.util.ArrayList;
import java.util.List;

public class MessageCenter {

    public static final String CLIENT_SENDBIRD = "sendbird";

    private static Client client;
    private static String LAST_CLIENT = CLIENT_SENDBIRD;
    private static List<String> notificationInboxMessages;

    /**
     *
     * @param context
     * @param connectionRequest
     * @param connectionInterface
     */
    public static void connect(Context context, ConnectionRequest connectionRequest, ConnectionInterface connectionInterface) {
        LAST_CLIENT = connectionRequest.getClient();
        client().getClient(connectionRequest.getClient()).connect(context, connectionRequest, connectionInterface);
    }

    /**
     *
     * @param chat_id
     */
    public static void join(Context context, String chat_id) {
        client().getClient(LAST_CLIENT).join(context, chat_id);
    }

    /**
     *
     * @param disconnectInterface
     */
    public static void disconnect(DisconnectInterface disconnectInterface) {
        client().getClient(LAST_CLIENT).disconnect( disconnectInterface);
    }

    /**
     *
     */
    public static void handleNotification(Context context, Class next, int icon, String title, RemoteMessage remoteMessage) {
        if (notificationInboxMessages == null)
            notificationInboxMessages = new ArrayList<>();
        client().getClient(LAST_CLIENT).handleNotification(context, next, icon, title, remoteMessage, notificationInboxMessages);
    }

    /**
     * Clears Notification Inbox Messages
     */
    public static void clearNotificationInboxMessages() {
        if (notificationInboxMessages != null) {
            notificationInboxMessages.clear();
        }
    }

    /**
     *
     * @return Client Factory
     */
    private static Client client() {
        if (client == null)
            client = new Client();
        return client;
    }

}
