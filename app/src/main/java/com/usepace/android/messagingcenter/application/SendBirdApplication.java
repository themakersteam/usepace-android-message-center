package com.usepace.android.messagingcenter.application;

import android.app.Application;

import com.sendbird.android.SendBird;

public class SendBirdApplication extends Application {

    public final static String APP_ID = "FE3AD311-7F0F-4E7E-9E22-25FF141A37C0";

    @Override
    public void onCreate() {
        super.onCreate();

        SendBird.init(APP_ID, this);
    }
}
