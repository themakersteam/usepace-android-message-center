package com.usepace.android.messagingcenter.interfaces;

import org.json.JSONObject;

public interface AppHandleNotificationInterface {
    void onMatched(JSONObject data); //Todo: Module the Json Object
    void onUnMatched();
}
