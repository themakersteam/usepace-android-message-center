package com.usepace.android.messagingcenter.network.sendbird;

/**
 * Created by Mohammed Nabil on 4/3/17.
 */

public interface SendBirdPlatformApiCallbackInterface<T> {

    void onSuccess(T result);
    void onError(String error);

}