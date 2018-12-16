package com.usepace.android.messagingcenter.clients.connection_client;


import android.app.Activity;
import android.content.Context;
import com.google.firebase.messaging.RemoteMessage;
import com.usepace.android.messagingcenter.interfaces.AppHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.CloseChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.interfaces.OpenChatViewInterface;
import com.usepace.android.messagingcenter.interfaces.SdkHandleNotificationInterface;
import com.usepace.android.messagingcenter.interfaces.UnReadMessagesInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.model.Theme;
import java.util.List;

abstract class ClientInterface {

    abstract public void connect(Context context, ConnectionRequest connectionRequest, ConnectionInterface connectionInterface);
    abstract public boolean isConnected();
    abstract public void getUnReadMessagesCount(Context context, String chat_id, UnReadMessagesInterface unReadMessagesInterface);
    abstract public void openChatView(Activity context, String chat_id, Theme theme, OpenChatViewInterface openChatViewInterface);
    abstract public void closeChatView(Context context, CloseChatViewInterface closeChatViewInterface);
    abstract public void sdkHandleNotification(Context context, Class next, int icon, String title, RemoteMessage remoteMessage, List<String> messages, SdkHandleNotificationInterface sdkHandleNotificationInterface);
    abstract public void appHandleNotification(RemoteMessage remoteMessage, AppHandleNotificationInterface appHandleNotificationInterface);
    abstract public void disconnect(Context context, DisconnectInterface disconnectInterface);
}
