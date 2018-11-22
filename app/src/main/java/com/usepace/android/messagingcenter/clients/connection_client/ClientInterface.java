package com.usepace.android.messagingcenter.clients.connection_client;


import android.content.Context;
import com.google.firebase.messaging.RemoteMessage;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.interfaces.DisconnectInterface;
import com.usepace.android.messagingcenter.interfaces.UnReadMessagesInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import java.util.List;

abstract class ClientInterface {

    abstract public void connect(Context context, ConnectionRequest connectionRequest, ConnectionInterface connectionInterface);
    abstract public boolean isConnected();
    abstract public void getUnReadMessagesCount(String chat_id, UnReadMessagesInterface unReadMessagesInterface);
    abstract public void join(Context context, String chat_id);
    abstract public void handleNotification(Context context,Class next,  int icon, String title, RemoteMessage remoteMessage, List<String> messages);
    abstract public void disconnect(DisconnectInterface disconnectInterface);
}
