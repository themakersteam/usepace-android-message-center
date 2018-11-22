package com.usepace.android.messagingcenter.clients.connection_client;

import android.content.Context;
import com.google.firebase.messaging.RemoteMessage;
import com.usepace.android.messagingcenter.exceptions.MessageCenterException;
import com.usepace.android.messagingcenter.interfaces.AppHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.CloseChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.interfaces.UnReadMessagesInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.model.Theme;
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
        try {
            client().getClient(connectionRequest.getClient()).connect(context, connectionRequest, connectionInterface);
        }
        catch (MessageCenterException e) {
            connectionInterface.onMessageCenterConnectionError(e.getCode(), e);
        }
    }

    /**
     *
     */
    public static boolean isConnected() {
        try {
            return client().getClient(LAST_CLIENT).isConnected();
        }
        catch (MessageCenterException e) {
            return false;
        }
    }


    /**
     *
     * @param chat_id
     * @param unReadMessagesInterface
     */
    public static void getUnReadMessagesCount(String chat_id, UnReadMessagesInterface unReadMessagesInterface) {
        try {
            client().getClient(LAST_CLIENT).getUnReadMessagesCount(chat_id, unReadMessagesInterface);
        }
        catch (MessageCenterException e) {
            unReadMessagesInterface.onErrorRetrievingMessages(e);
        }
    }

    /**
     *
     * @param chat_id
     */
    public static void openChatView(Context context, String chat_id, Theme theme) {
        try {
            client().getClient(LAST_CLIENT).openChatView(context, chat_id, theme);
        }
        catch (MessageCenterException e) {
        }
    }


    /**
     *
     * @param context
     */
    public static void closeChatView(Context context, CloseChatViewInterface closeChatViewInterface) {
        try {
            client().getClient(LAST_CLIENT).closeChatView(context, closeChatViewInterface);
        }
        catch (MessageCenterException e) {
        }
    }

    /**
     *
     * @param disconnectInterface
     */
    public static void disconnect(DisconnectInterface disconnectInterface) {
        try {
            client().getClient(LAST_CLIENT).disconnect(disconnectInterface);
        }
        catch (MessageCenterException e) {
        }
    }

    /**
     *
     */
    public static void sdkHandleNotification(Context context, Class next, int icon, String title, RemoteMessage remoteMessage) {
        try {
            if (notificationInboxMessages == null)
                notificationInboxMessages = new ArrayList<>();
            client().getClient(LAST_CLIENT).sdkHandleNotification(context, next, icon, title, remoteMessage, notificationInboxMessages);
        }
        catch (MessageCenterException e) {

        }
    }

    /**
     *
     */
    public static void appHandleNotification(RemoteMessage remoteMessage, AppHandleNotificationInterface appHandleNotificationInterface) {
        try {
            client().getClient(LAST_CLIENT).appHandleNotification(remoteMessage, appHandleNotificationInterface);
        }
        catch (MessageCenterException e) {
            if (appHandleNotificationInterface != null) {
                appHandleNotificationInterface.onUnMatched();
            }
        }
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
