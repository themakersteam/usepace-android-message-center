package com.usepace.android.messagingcenter.application;

import android.app.Application;

import com.sendbird.android.SendBird;

public class SendBirdApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SendBird.init("", this);
    }
}
