package com.usepace.android.messagingcenter.exceptions;

public class MessageCenterException extends Exception{

    protected int code;

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public MessageCenterException(String message) {
        super(message);
        this.setCode(0);
    }

    public MessageCenterException(String message, int code) {
        super(message);
        this.setCode(code);
    }

}
