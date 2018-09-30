package com.usepace.android.messagingcenter.clients.connection_client;

class Client {

    protected ClientInterface getClient(String type) {
        if (type.equalsIgnoreCase(MessageCenter.CLIENT_SENDBIRD))
            return SendBirdClient.Instance();
        return null;
    }
}
