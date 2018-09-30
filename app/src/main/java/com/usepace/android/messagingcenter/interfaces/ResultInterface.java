package com.usepace.android.messagingcenter.interfaces;

public interface ResultInterface {
    void onSuccess();
    void onFailed(int code, String message);
}
