package com.usepace.android.messagingcenter.clients.connection_client;

import com.usepace.android.messagingcenter.exceptions.MessageCenterException;

class Client {

    protected ClientInterface getClient(String type) throws MessageCenterException {
        if (type == null) {
            throw new MessageCenterException("No Client Type Provided");
        }
        else if (type.equalsIgnoreCase(MessageCenter.CLIENT_SENDBIRD))
            return SendBirdClient.Instance();
        else
            throw new MessageCenterException("Client Not found !");
    }
}
