package com.usepace.android.messagingcenter.interfaces;

public interface OnCallButtonClickedResult {
    void onSuccess(String phone_number);
    void onFailure(String error_message);
}
