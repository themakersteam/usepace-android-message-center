package com.usepace.android.messagingcenter.screens.sendbird;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.sendbird.android.SendBird;
import com.usepace.android.messagingcenter.Enum.EventForApp;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.clients.connection_client.MessageCenter;
import com.usepace.android.messagingcenter.interfaces.OnCallButtonClickedResult;
import com.usepace.android.messagingcenter.model.Theme;
import com.usepace.android.messagingcenter.utils.DeviceUtils;
import com.usepace.android.messagingcenter.utils.LoadingUtils;
import com.usepace.android.messagingcenter.utils.PreferenceUtils;
import java.util.HashMap;
import java.util.Map;


public class SendBirdChatActivity extends AppCompatActivity{


    private onBackPressedListener mOnBackPressedListener;
    private Toolbar toolbar;
    private TextView toolbarSubtitle;
    private Theme theme;
    private Menu menu;
    private LoadingUtils loadingUtils;
    private boolean channel_frozen = false;
    public static String PACKAGE_NAME;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_channel);
        init();
    }

    private void init() {
        theme = getIntent().hasExtra("THEME") ? (Theme) getIntent().getExtras().getParcelable("THEME") : null;
        loadingUtils = new LoadingUtils(this);

        try {
            SendBird.setAutoBackgroundDetection(true);
        }
        catch (RuntimeException e) {e.printStackTrace();}

        PreferenceUtils.init(this);
        initToolBar();
        String channelUrl = getIntent().getStringExtra("CHANNEL_URL");

        if(channelUrl != null) {
            MessageCenter.clearNotificationInboxMessages(channelUrl);
            Fragment fragment = SendBirdChatFragment.newInstance(channelUrl);
            Bundle bundle = new Bundle();
            bundle.putString("CHANNEL_URL", channelUrl);
            if (getIntent() != null && getIntent().getExtras() != null) {
                bundle.putAll(getIntent().getExtras());
            }
            fragment.setArguments(bundle);
            FragmentManager manager = getSupportFragmentManager();
            manager.popBackStack();
            manager.beginTransaction().replace(R.id.container_group_channel, fragment).commit();
        }
        else {
            finish();
        }
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.my_toolbar);
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(theme != null && theme.getToolbarTitle() != null ? theme.getToolbarTitle() : getString(R.string.message_center_toolbar_title));
        }
        toolbarSubtitle.setText(theme != null && theme.getToolbarSubtitle() != null ? theme.getToolbarSubtitle() : "");
        PACKAGE_NAME = getIntent().getStringExtra("PACKAGE_NAME");
    }

    public void setOnBackPressedListener(onBackPressedListener listener) {
        mOnBackPressedListener = listener;
    }

    public void freeze() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    if (menu != null && menu.findItem(R.id.menu_action_call) != null && theme != null && theme.isCallEnabled()) {
                        menu.findItem(R.id.menu_action_call).setIcon(ContextCompat.getDrawable(SendBirdChatActivity.this, R.drawable.ic_calldisabled));
                        channel_frozen = true;
                    }
                }
            }
        }, 150); // Making sure the menu is loaded
    }

    private void callRequested() {
        onEvent(EventForApp.HungerStation.toString(), "call_rider.clicked", new HashMap<String, Object>());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ((SendBirdChatFragment)getSupportFragmentManager().findFragmentById(R.id.container_group_channel)).requestCallPermissions();
        }
        else {
            showCallDialog();
        }
    }

    private void onEvent(String app_name, String key, Map<String, Object> data) {
        if (MessageCenter.sdkCallbacks != null) {
            MessageCenter.sdkCallbacks.onEvent(app_name, key, data);
        }
    }

    private void showCallDialog() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.ms_call_message))
                .setPositiveButton(R.string.ms_call, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (MessageCenter.sdkCallbacks == null)
                            return;
                        onEvent(EventForApp.HungerStation.toString(), "call_rider.submitted", new HashMap<String, Object>());
                        loadingUtils.showOnScreenLoading();
                        MessageCenter.sdkCallbacks.onCallButtonClicked(new OnCallButtonClickedResult() {
                            @Override
                            public void onSuccess(String phone_number) {
                                loadingUtils.hideOnScreenLoading();
                                DeviceUtils.call(SendBirdChatActivity.this, phone_number);
                            }

                            @Override
                            public void onFailure(String error_message) {
                                loadingUtils.hideOnScreenLoading();
                                Toast.makeText(SendBirdChatActivity.this, error_message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.ms_decline, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onEvent(EventForApp.HungerStation.toString(), "call_rider.canceled", new HashMap<String, Object>());
                    }
                }).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.hasExtra("close") && intent.getBooleanExtra("close", false)) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceUtils.init(this);
    }

    @Override
    public void onBackPressed() {
        setResult(MessageCenter.OPEN_CHAT_VIEW_RESPONSE_CODE);
        if (mOnBackPressedListener != null && mOnBackPressedListener.onBack()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        setResult(MessageCenter.OPEN_CHAT_VIEW_RESPONSE_CODE);
    }

    @Override
    protected void onDestroy() {
        setResult(MessageCenter.OPEN_CHAT_VIEW_RESPONSE_CODE);
        super.onDestroy();


        try
        {
          SendBird.disconnect(new SendBird.DisconnectHandler() {
            @Override
            public void onDisconnected() {

            }
        });

        }catch (Exception exp)
        {

        }





    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        if (theme != null && theme.isCallEnabled()) {
            getMenuInflater().inflate(R.menu.menu_with_call, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else if (id == R.id.menu_action_call && !channel_frozen) { //Handle Call
            callRequested();
        }
        return super.onOptionsItemSelected(item);
    }

    public interface onBackPressedListener {
        boolean onBack();
    }
}