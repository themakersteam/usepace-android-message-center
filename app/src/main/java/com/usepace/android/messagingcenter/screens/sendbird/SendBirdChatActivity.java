package com.usepace.android.messagingcenter.screens.sendbird;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.sendbird.android.SendBird;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.clients.connection_client.MessageCenter;


public class SendBirdChatActivity extends AppCompatActivity{


    private onBackPressedListener mOnBackPressedListener;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_channel);
        MessageCenter.clearNotificationInboxMessages();
        init();
    }

    private void init() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.message_center_toolbar_title));
        }
        String channelUrl = getIntent().getStringExtra("CHANNEL_URL");

        if(channelUrl != null) {
            Fragment fragment = SendBirdChatFragment.newInstance(channelUrl);
            Bundle bundle = new Bundle();
            bundle.putString("CHANNEL_URL", channelUrl);
            fragment.setArguments(bundle);
            FragmentManager manager = getSupportFragmentManager();
            manager.popBackStack();
            manager.beginTransaction().replace(R.id.container_group_channel, fragment).commit();
        }
        else {
            finish();
        }
    }


    public void setOnBackPressedListener(onBackPressedListener listener) {
        mOnBackPressedListener = listener;
    }

    @Override
    public void onBackPressed() {
        if (mOnBackPressedListener != null && mOnBackPressedListener.onBack()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SendBird.disconnect(new SendBird.DisconnectHandler() {
            @Override
            public void onDisconnected() {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public interface onBackPressedListener {
        boolean onBack();
    }
}