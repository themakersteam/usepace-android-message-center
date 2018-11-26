package com.usepace.android.messagingcenter.screens.test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.usepace.android.messagingcenter.clients.connection_client.MessageCenter;
import com.usepace.android.messagingcenter.exceptions.MessageCenterException;
import com.usepace.android.messagingcenter.interfaces.ConnectionInterface;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import com.usepace.android.messagingcenter.model.Theme;

public class TestActivity extends AppCompatActivity{

    private String chat_id = "sendbird_group_channel_2456028_f4a5055d72e15074e5832cd3d60d5fa662980e84";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MessageCenter.connect(this, prepareRequest(), new ConnectionInterface() {
            @Override
            public void onMessageCenterConnected() {
                MessageCenter.openChatView(TestActivity.this, chat_id, new Theme("Test Title", "#12345678 • Provider name", ":"));
            }

            @Override
            public void onMessageCenterConnectionError(int code, MessageCenterException e) {

            }
        });
    }

    private ConnectionRequest prepareRequest() {
        ConnectionRequest connectionRequest = new ConnectionRequest();
        connectionRequest.setAppId("FE3AD311-7F0F-4E7E-9E22-25FF141A37C0");
        connectionRequest.setClient(MessageCenter.CLIENT_SENDBIRD);
        connectionRequest.setUserId("customer_hs_184890");
        connectionRequest.setAccessToken("8b21b79c6a07d74e95cf6c91837ec2a64e9cbc54");
        connectionRequest.setFcmToken("testo");
        return connectionRequest;
    }
}
