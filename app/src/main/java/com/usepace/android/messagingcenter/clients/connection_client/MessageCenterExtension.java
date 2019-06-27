package com.usepace.android.messagingcenter.clients.connection_client;

import android.app.Activity;
import android.content.Context;

import com.google.firebase.messaging.RemoteMessage;
import com.sendbird.android.SendBird;
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

public class MessageCenterExtension {

    public static final String CLIENT_SENDBIRD = "sendbird";

    private static Client client;
    private static String LAST_CLIENT = CLIENT_SENDBIRD;



    public static void reInitClient(Context context) {
        try {
            SendBirdClient.Instance().reInit(context);
        }
        catch (Exception e) {
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
            SendBirdClient.Instance().reConnect();
        }
        catch (Exception e) {
            //connectionInterface.onMessageCenterConnectionError(e.getCode(), e);
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
