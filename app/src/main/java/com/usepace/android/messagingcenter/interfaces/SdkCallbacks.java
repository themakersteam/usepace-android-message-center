package com.usepace.android.messagingcenter.interfaces;

import java.util.Map;

public interface SdkCallbacks {
    void onCallButtonClicked(OnCallButtonClickedResult onCallButtonClickedResult);
    void onEvent(String app_name, String event_key, Map<String,Object> data);
}
