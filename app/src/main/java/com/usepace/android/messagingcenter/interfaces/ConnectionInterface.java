package com.usepace.android.messagingcenter.interfaces;

public interface ConnectionInterface {
    void onMessageCenterConnected();
    void onMessageCenterConnectionError(int code, String message);
}
