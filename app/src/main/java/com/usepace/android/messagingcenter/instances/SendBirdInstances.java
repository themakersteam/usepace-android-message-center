package com.usepace.android.messagingcenter.instances;

public class SendBirdInstances {

    private static SendBirdInstances instances = null;

    //Vars
    private boolean chatViewOpened = false;


    public void chatViewOpened() {
        chatViewOpened = true;
    }

    public void chatViewClosed() {
        chatViewOpened = false;
    }

    public boolean isChatViewOpened() {
        return chatViewOpened;
    }

    /**
     **/
    public static SendBirdInstances instance() {
        if (instances == null) {
            instances = new SendBirdInstances();
        }
        return instances;
    }
}
