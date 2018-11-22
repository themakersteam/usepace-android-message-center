package com.usepace.android.messagingcenter.interfaces;

import com.usepace.android.messagingcenter.exceptions.MessageCenterException;

public interface UnReadMessagesInterface {
    void onUnreadMessages(int count);
    void onErrorRetrievingMessages(MessageCenterException e);
}
