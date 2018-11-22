package com.usepace.android.messagingcenter.exceptions;

public class MessageCenterException extends RuntimeException{

    public MessageCenterException(String message) {
        super(message);
    }

    public MessageCenterException(String message, Throwable cause) {
        super(message, cause);
    }

}
