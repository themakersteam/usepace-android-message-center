package com.usepace.android.messagingcenter.clients.connection_client;

import android.app.Activity;
import android.content.Context;
import com.google.firebase.messaging.RemoteMessage;
import com.usepace.android.messagingcenter.exceptions.MessageCenterException;
import com.usepace.android.messagingcenter.interfaces.AppHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.CloseChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.interfaces.OpenChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.SdkCallbacks;
import com.usepace.android.messagingcenter.interfaces.SdkHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.UnReadMessagesInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.model.Theme;
import java.util.HashMap;
import java.util.List;

public class MessageCenter {

    public static final String CLIENT_SENDBIRD = "sendbird";
    public static final Integer OPEN_CHAT_VIEW_REQUEST_CODE = 234;
    public static final Integer OPEN_CHAT_VIEW_RESPONSE_CODE = 232;
    public static SdkCallbacks sdkCallbacks = null;

    private static Client client;
    private static String LAST_CLIENT = CLIENT_SENDBIRD;
    private static HashMap<String, List<String>> notificationInboxMessages;

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
     * @param context
     *
     *
     */
    public static void reInitClient(Context context) {
        try {
            client().getClient(LAST_CLIENT).reInit(context);
        }
        catch (MessageCenterException e) {
            //connectionInterface.onMessageCenterConnectionError(e.getCode(), e);
        }
    }




    /**
     *
     *
     *
     *
     */
    public static void reConnect() {
        try {
            client().getClient(LAST_CLIENT).reConnect();
        }
        catch (MessageCenterException e) {
            //connectionInterface.onMessageCenterConnectionError(e.getCode(), e);
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
     * @param context
     * @param chat_id
     * @param unReadMessagesInterface
     */
    public static void getUnReadMessagesCount(Context context, String chat_id, UnReadMessagesInterface unReadMessagesInterface) {
        try {
            client().getClient(LAST_CLIENT).getUnReadMessagesCount(context, chat_id, unReadMessagesInterface);
        }
        catch (MessageCenterException e) {
            unReadMessagesInterface.onErrorRetrievingMessages(e);
        }
    }

    /**
     *
     * @param context
     * @param optionalConnectionRequest
     * @param chat_id
     * @param theme
     * @param openChatViewInterface
     */
    public static void openChatView(Activity context, ConnectionRequest optionalConnectionRequest, String chat_id, Theme theme, OpenChatViewInterface openChatViewInterface, SdkCallbacks optionalSdkCallback) {
        try {
            sdkCallbacks = optionalSdkCallback;
            client().getClient(LAST_CLIENT).openChatView(context, optionalConnectionRequest, chat_id, theme, openChatViewInterface);
        }
        catch (MessageCenterException e) {
            if (openChatViewInterface != null) {
                openChatViewInterface.onError(e);
            }
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
    public static void disconnect(Context context, DisconnectInterface disconnectInterface) {
        try {
            client().getClient(LAST_CLIENT).disconnect(context, disconnectInterface);
        }
        catch (MessageCenterException e) {
        }
    }

    /**
     *
     */
    public static void sdkHandleNotification(Context context, Class next, int icon, String title, RemoteMessage remoteMessage, SdkHandleNotificationInterface sdkHandleNotificationInterface) {
        try {
            if (notificationInboxMessages == null)
                notificationInboxMessages = new HashMap<>();
            client().getClient(LAST_CLIENT).sdkHandleNotification(context, next, icon, title, remoteMessage, notificationInboxMessages, sdkHandleNotificationInterface);
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
    public static void clearNotificationInboxMessages(String channel_url) {
        if (notificationInboxMessages != null && notificationInboxMessages.containsKey(channel_url) && notificationInboxMessages.get(channel_url) != null) {
            notificationInboxMessages.get(channel_url).clear();
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
