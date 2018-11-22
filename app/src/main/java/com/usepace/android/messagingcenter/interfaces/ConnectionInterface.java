package com.usepace.android.messagingcenter.interfaces;

import com.usepace.android.messagingcenter.exceptions.MessageCenterException;

public interface ConnectionInterface {
    void onMessageCenterConnected();
    void onMessageCenterConnectionError(int code, MessageCenterException e);
}
