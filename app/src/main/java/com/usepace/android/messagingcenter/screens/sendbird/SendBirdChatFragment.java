package com.usepace.android.messagingcenter.screens.sendbird;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidadvance.topsnackbar.TSnackbar;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.Member;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.model.SendBirdMessage;
import com.usepace.android.messagingcenter.screens.mediaplayer.MediaPlayerActivity;
import com.usepace.android.messagingcenter.screens.myLocation.MyLocationActivity;
import com.usepace.android.messagingcenter.screens.photoviewer.PhotoViewerActivity;
import com.usepace.android.messagingcenter.screens.sendfile.SendFileActivity;
import com.usepace.android.messagingcenter.utils.ConnectionManager;
import com.usepace.android.messagingcenter.utils.FileUtils;
import com.usepace.android.messagingcenter.utils.TextUtils;
import com.usepace.android.messagingcenter.utils.UrlPreviewInfo;
import com.usepace.android.messagingcenter.utils.WebUtils;
import org.json.JSONException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


public class SendBirdChatFragment extends Fragment {

    private static final String LOG_TAG =  SendBirdChatFragment.class.getSimpleName();

    private static final int CHANNEL_LIST_LIMIT = 30;
    private static final String CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_GROUP_CHAT";
    private static  String CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_CHAT";

    private static final int STATE_NORMAL = 0;
    private static final int STATE_EDIT = 1;

    private static final String STATE_CHANNEL_URL = "STATE_CHANNEL_URL";
    private static final int INTENT_REQUEST_CHOOSE_MEDIA = 301;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 13;
    private static final int PERMISSION_CAMERA = 14;
    private static final int SEND_FILE_ACTIVITY_RESULT = 101;
    private static final int OPEN_LOCATION_ACTIVITY_RESULT = 102;

    private InputMethodManager mIMM;
    private HashMap<BaseChannel.SendFileMessageWithProgressHandler, FileMessage> mFileProgressHandlerMap;
    private HashMap<String, Boolean> inProgressMessages;

    private RelativeLayout mRootLayout;
    private FileOptionsBottomSheetAdapter mBottomSheetAdapter;
    private RecyclerView mRecyclerView;
    private SendBirdChatAdapter mChatAdapter;
    private LinearLayoutManager mLayoutManager;
    private EditText mMessageEditText;
    private ImageView mMessageSendButton;
    private ImageView mMessageCameraButton;
    private ImageButton mUploadFileButton;
    private View mCurrentEventLayout;
    private TextView mCurrentEventText;
    private LinearLayout groupChatBox;
    private TextView welcomeMessage;

    private GroupChannel mChannel;
    private String mChannelUrl;
    private boolean showWelcome = true;

    private int mCurrentState = STATE_NORMAL;
    private BaseMessage mEditingMessage = null;

    private final int channel_frozen_key = 900050;


    /**
     * To create an instance of this fragment, a Channel URL should be required.
     */
    public static SendBirdChatFragment newInstance(@NonNull String channelUrl) {
        SendBirdChatFragment fragment = new SendBirdChatFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIMM = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mFileProgressHandlerMap = new HashMap<>();
        inProgressMessages = new HashMap<>();

        mChannelUrl = getArguments().getString("CHANNEL_URL");
        CHANNEL_HANDLER_ID = mChannelUrl;


        mChatAdapter = new SendBirdChatAdapter(getActivity(), getArguments());
        setUpChatListAdapter();

        // Load messages from cache.
        boolean isChannelValid = mChatAdapter.load(mChannelUrl);
        if (!isChannelValid) {
            freeze();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_group_chat, container, false);

        setRetainInstance(true);

        mRootLayout = (RelativeLayout) rootView.findViewById(R.id.layout_group_chat_root);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_group_chat);

        mCurrentEventLayout = rootView.findViewById(R.id.layout_group_chat_current_event);
        mCurrentEventText = (TextView) rootView.findViewById(R.id.text_group_chat_current_event);

        mMessageEditText = (EditText) rootView.findViewById(R.id.edittext_group_chat_message);
        mMessageSendButton = (ImageView) rootView.findViewById(R.id.button_group_chat_send);
        mMessageCameraButton = (ImageView)rootView.findViewById(R.id.button_camera_send);
        mUploadFileButton = (ImageButton) rootView.findViewById(R.id.button_group_chat_upload);
        welcomeMessage = (TextView) rootView.findViewById(R.id.text_group_chat_welcome);

        if (getArguments() != null && getArguments().containsKey("WELCOME_MESSAGE")) {
            welcomeMessage.setText(getArguments().getString("WELCOME_MESSAGE"));
        }
        else {
            welcomeMessage.setVisibility(View.GONE);
            showWelcome = false;
        }

        groupChatBox = rootView.findViewById(R.id.layout_group_chat_chatbox);

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mMessageSendButton.setVisibility(s.toString().replaceAll(" ", "").replaceAll("\n", "").length() > 0 ? View.VISIBLE : View.GONE);
                mMessageCameraButton.setVisibility(s.toString().replaceAll(" ", "").replaceAll("\n", "").length() > 0 ? View.GONE : View.VISIBLE);
            }
        });

        mMessageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState == STATE_EDIT) {
                    String userInput = mMessageEditText.getText().toString();
                    if (userInput.length() > 0) {
                        if (mEditingMessage != null) {
                            editMessage(mEditingMessage, userInput);
                        }
                    }
                    setState(STATE_NORMAL, null, -1);
                } else {
                    String userInput = mMessageEditText.getText().toString();
                    if (userInput.length() > 0) {
                        sendUserMessage(userInput);
                        mMessageEditText.setText("");
                    }
                }
            }
        });

        mMessageCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMedia(SendFileActivity.REQUEST_IMAGE_CAPTURE);
            }
        });

        mUploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileOptions();
            }
        });

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    setTypingStatus(false);
                }
                else {
                    setTypingStatus(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setUpRecyclerView();
        setHasOptionsMenu(true);

        return rootView;
    }

    private void freeze() {
        if (groupChatBox != null) {
            groupChatBox.setVisibility(View.GONE);
        }
    }

    private void openSendFileScreen(int action) {
        startActivityForResult(new Intent(getActivity(), SendFileActivity.class).putExtra("ACTION", action), SEND_FILE_ACTIVITY_RESULT);
    }

    private void refresh() {
        if (mChannel == null) {
            GroupChannel.getChannel(mChannelUrl, new GroupChannel.GroupChannelGetHandler() {
                @Override
                public void onResult(GroupChannel groupChannel, SendBirdException e) {
                    if (e != null) {
                        // Error!
                        e.printStackTrace();
                        if (e.getCode() == channel_frozen_key) {
                            freeze();
                        }
                        return;
                    }
                    mChannel = groupChannel;
                    mChannel.setPushPreference(true, null);
                    mChatAdapter.setChannel(mChannel);
                    mChatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                        @Override
                        public void onResult(List<BaseMessage> list, SendBirdException e) {
                            mChatAdapter.markAllMessagesAsRead();
                        }
                    });
                    if (mChannel.isFrozen()) {
                        freeze();
                    }
                }
            });
        } else {
            mChannel.refresh(new GroupChannel.GroupChannelRefreshHandler() {
                @Override
                public void onResult(SendBirdException e) {
                    if (e != null) {
                        // Error!
                        e.printStackTrace();
                        if (e.getCode() == channel_frozen_key) {
                            freeze();
                        }
                        return;
                    }
                    mChatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                        @Override
                        public void onResult(List<BaseMessage> list, SendBirdException e) {
                            mChatAdapter.markAllMessagesAsRead();
                        }
                    });
                    if (mChannel.isFrozen()) {
                        freeze();
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ConnectionManager.addConnectionManagementHandler(CONNECTION_HANDLER_ID, new ConnectionManager.ConnectionManagementHandler() {
            @Override
            public void onConnected(boolean reconnect) {
                refresh();
            }
        });

        mChatAdapter.setContext(getActivity()); // Glide bug fix (java.lang.IllegalArgumentException: You cannot start a load for a destroyed activity)

        // Gets channel from URL user requested

        Log.d(LOG_TAG, mChannelUrl);

        SendBird.addChannelHandler(CHANNEL_HANDLER_ID, new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
                if (baseChannel.getUrl().equals(mChannelUrl)) {
                    mChatAdapter.markAllMessagesAsRead();
                    // Add new message to view
                    mChatAdapter.addFirst(baseMessage);
                }
                else {
                    String user_name = "";
                    if (baseMessage instanceof UserMessage) {
                        user_name = ((UserMessage) baseMessage).getSender().getNickname();
                    } else if (baseMessage instanceof FileMessage) {
                        user_name = ((UserMessage) baseMessage).getSender().getNickname();
                    }
                    showOtherChannelMessageSnack(getString(R.string.message_center_new_message_from) + " " + user_name);
                }
            }

            @Override
            public void onMessageDeleted(BaseChannel baseChannel, long msgId) {
                super.onMessageDeleted(baseChannel, msgId);
                if (baseChannel.getUrl().equals(mChannelUrl)) {
                    mChatAdapter.delete(msgId);
                }
            }

            @Override
            public void onMessageUpdated(BaseChannel channel, BaseMessage message) {
                super.onMessageUpdated(channel, message);
                if (channel.getUrl().equals(mChannelUrl)) {
                    mChatAdapter.update(message);
                }
            }

            @Override
            public void onReadReceiptUpdated(GroupChannel channel) {
                if (channel.getUrl().equals(mChannelUrl)) {
                    mChatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTypingStatusUpdated(GroupChannel channel) {
                if (channel.getUrl().equals(mChannelUrl)) {
                    List<Member> typingUsers = channel.getTypingMembers();
                    displayTyping(typingUsers);
                }
            }

        });
    }

    @Override
    public void onPause() {
        setTypingStatus(false);

        ConnectionManager.removeConnectionManagementHandler(CONNECTION_HANDLER_ID);
        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Save messages to cache.
        mChatAdapter.save();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_CHANNEL_URL, mChannelUrl);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Set this as true to restore background connection management.
        SendBird.setAutoBackgroundDetection(true);
        if ((requestCode == INTENT_REQUEST_CHOOSE_MEDIA || requestCode == SEND_FILE_ACTIVITY_RESULT) && resultCode == Activity.RESULT_OK) {
            // If user has successfully chosen the image, show a dialog to confirm upload.
            if (data == null) {
                Log.d(LOG_TAG, "data is null!");
                return;
            }
            sendFileWithThumbnail(data.getData());
            if (data.hasExtra("CAPTION")) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendUserMessage(data.getStringExtra("CAPTION"));
                    }
                }, 750);
            }
        }
        else if (requestCode == OPEN_LOCATION_ACTIVITY_RESULT && resultCode == Activity.RESULT_OK) {
            if (data != null && data.hasExtra("lat") && data.hasExtra("lng")) {
                sendUserMessage("location://?lat=" + data.getDoubleExtra("lat", 0) + "&long=" + data.getDoubleExtra("lng", 0));
            }
        }
    }

    private void setUpRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mChatAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mChatAdapter.getItemCount() >= 10 && mLayoutManager.findFirstVisibleItemPosition() == 0 && showWelcome) {
                    welcomeMessage.setVisibility(View.GONE);
                }
                else if (showWelcome){
                    welcomeMessage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() == mChatAdapter.getItemCount() - 1) {
                    mChatAdapter.loadPreviousMessages(CHANNEL_LIST_LIMIT, null);
                }
            }
        });
    }

    private void showFileOptions() {
        if(mBottomSheetAdapter != null && mBottomSheetAdapter.isAdded()) {
            mBottomSheetAdapter.dismiss();
        }
        else {
            mBottomSheetAdapter = new FileOptionsBottomSheetAdapter().newInstance();
            mBottomSheetAdapter.setListener(new FileOptionsBottomSheetAdapter.ButtonClickListener() {
                @Override
                public void onCameraClicked() {
                    mBottomSheetAdapter.dismiss();
                    requestMedia(SendFileActivity.REQUEST_IMAGE_CAPTURE);
                }

                @Override
                public void onGalleryClicked() {
                    mBottomSheetAdapter.dismiss();
                    requestMedia(SendFileActivity.REQUEST_GALLERY_CAPTURE);
                }

                @Override
                public void onLocationClicked() {
                    startActivityForResult(new Intent(getActivity(), MyLocationActivity.class), OPEN_LOCATION_ACTIVITY_RESULT);
                    mBottomSheetAdapter.dismiss();
                }
            });
            mBottomSheetAdapter.show(getActivity().getSupportFragmentManager(), mBottomSheetAdapter.getTag());
        }
    }

    private void setUpChatListAdapter() {
        mChatAdapter.setItemClickListener(new SendBirdChatAdapter.OnItemClickListener() {
            @Override
            public void onUserMessageItemClick(UserMessage message) {
                // Restore failed message and remove the failed message from list.
                if (mChatAdapter.isFailedMessage(new SendBirdMessage(message))) {
                    retryFailedMessage(message);
                    return;
                }

                // Message is sending. Do nothing on click event.
                if (mChatAdapter.isTempMessage(new SendBirdMessage(message))) {
                    return;
                }


                if (message.getCustomType().equals(SendBirdChatAdapter.URL_PREVIEW_CUSTOM_TYPE)) {
                    try {
                        UrlPreviewInfo info = new UrlPreviewInfo(message.getData());
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.getUrl()));
                        try {
                            startActivity(browserIntent);
                        }
                        catch (ActivityNotFoundException e) {

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else if (message.getMessage().startsWith("location://")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TextUtils.getLocationUrlMessageIfExists(message.getMessage())));
                    try {
                        startActivity(browserIntent);
                    }
                    catch (ActivityNotFoundException e){
                    }
                }
            }

            @Override
            public void onFileMessageItemClick(FileMessage message) {
                // Load media chooser and remove the failed message from list.
                if (mChatAdapter.isFailedMessage(new SendBirdMessage(message))) {
                    retryFailedMessage(message);
                    return;
                }

                // Message is sending. Do nothing on click event.
                if (mChatAdapter.isTempMessage(new SendBirdMessage(message))) {
                    return;
                }


                onFileMessageClicked(message);
            }
        });

        mChatAdapter.setItemLongClickListener(new SendBirdChatAdapter.OnItemLongClickListener() {
            @Override
            public void onUserMessageItemLongClick(UserMessage message, int position) {
                //Disabling edit/delete options
                //showMessageOptionsDialog(message, position);
            }

            @Override
            public void onFileMessageItemLongClick(FileMessage message) {
            }

            @Override
            public void onAdminMessageItemLongClick(AdminMessage message) {
            }
        });
    }

    private void showMessageOptionsDialog(final BaseMessage message, final int position) {
        String[] options = new String[] { getString(R.string.edit_message), getString(R.string.delete_message_option) };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    setState(STATE_EDIT, message, position);
                } else if (which == 1) {
                    deleteMessage(message);
                }
            }
        });
        builder.create().show();
    }

    private void setState(int state, BaseMessage editingMessage, final int position) {
        switch (state) {
            case STATE_NORMAL:
                mCurrentState = STATE_NORMAL;
                mEditingMessage = null;

                mUploadFileButton.setVisibility(View.VISIBLE);
                mMessageEditText.setText("");
                break;

            case STATE_EDIT:
                mCurrentState = STATE_EDIT;
                mEditingMessage = editingMessage;

                mUploadFileButton.setVisibility(View.GONE);
                String messageString = ((UserMessage)editingMessage).getMessage();
                if (messageString == null) {
                    messageString = "";
                }
                mMessageEditText.setText(messageString);
                if (messageString.length() > 0) {
                    mMessageEditText.setSelection(0, messageString.length());
                }

                mMessageEditText.requestFocus();
                mMessageEditText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIMM.showSoftInput(mMessageEditText, 0);

                        mRecyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mRecyclerView.scrollToPosition(position);
                            }
                        }, 500);
                    }
                }, 100);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((SendBirdChatActivity)context).setOnBackPressedListener(new SendBirdChatActivity.onBackPressedListener() {
            @Override
            public boolean onBack() {
                if (mCurrentState == STATE_EDIT) {
                    setState(STATE_NORMAL, null, -1);
                    return true;
                }
                else if (inProgressMessages != null && inProgressMessages.size() > 0) {
                    showInProgressMessageAlert();
                    return true;
                }
                else {
                    mIMM.hideSoftInputFromWindow(mMessageEditText.getWindowToken(), 0);
                    return false;
                }
            }
        });
    }

    private void retryFailedMessage(final BaseMessage message) {
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.retry))
                .setPositiveButton(R.string.resend_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (message instanceof UserMessage) {
                                String userInput = ((UserMessage) message).getMessage();
                                sendUserMessage(userInput);
                            } else if (message instanceof FileMessage) {
                                Uri uri = mChatAdapter.getTempFileMessageUri(new SendBirdMessage(message));
                                sendFileWithThumbnail(uri);
                            }
                            mChatAdapter.removeFailedMessage(message);
                        }
                    }
                }).show();
    }

    /**
     * Display which users are typing.
     * If more than two users are currently typing, this will state that "multiple users" are typing.
     *
     * @param typingUsers The list of currently typing users.
     */
    private void displayTyping(List<Member> typingUsers) {

        if (typingUsers.size() > 0) {
            mCurrentEventLayout.setVisibility(View.VISIBLE);
            String string;

            if (typingUsers.size() == 1) {
                string = typingUsers.get(0).getNickname() + " " + getString(R.string.is_typing);
            } else if (typingUsers.size() == 2) {
                string = typingUsers.get(0).getNickname() + " " + typingUsers.get(1).getNickname() + getString(R.string.is_typing);
            } else {
                string = getString(R.string.multiple_users_are_typing);
            }
            string = string + "...";
            mCurrentEventText.setText(string);
        } else {
            mCurrentEventLayout.setVisibility(View.GONE);
        }
    }

    private void requestMedia(int request) {
        if (request == SendFileActivity.REQUEST_GALLERY_CAPTURE && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions();
        }
        else if (request == SendFileActivity.REQUEST_IMAGE_CAPTURE &&
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermissions();
        }
        else {
            openSendFileScreen(request);
        }
    }

    private void requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Snackbar.make(mRootLayout, getString(R.string.storage_access_permission_needed),
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.ok), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSION_WRITE_EXTERNAL_STORAGE);
                        }
                    })
                    .show();
        } else {
            // Permission has not been granted yet. Request it directly.
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }


    private void requestCameraPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
            Snackbar.make(mRootLayout, getString(R.string.camera_access_permission_needed),
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.ok), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSION_CAMERA);
                        }
                    })
                    .show();
        } else {
            // Permission has not been granted yet. Request it directly.
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_CAMERA);
        }
    }

    private void onFileMessageClicked(FileMessage message) {
        String type = message.getType().toLowerCase();
        if (type.startsWith("image")) {
            Intent i = new Intent(getActivity(), PhotoViewerActivity.class);
            i.putExtra("url", message.getUrl());
            i.putExtra("type", message.getType());
            startActivity(i);
        } else if (type.startsWith("video")) {
            Intent intent = new Intent(getActivity(), MediaPlayerActivity.class);
            intent.putExtra("url", message.getUrl());
            startActivity(intent);
        } else {
            showDownloadConfirmDialog(message);
        }
    }

    private void showDownloadConfirmDialog(final FileMessage message) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // If storage permissions are not granted, request permissions at run-time,
            // as per < API 23 guidelines.
            requestStoragePermissions();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.download_file))
                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                FileUtils.downloadFile(getActivity(), message.getUrl(), message.getName());
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null).show();
        }

    }

    private void sendUserMessageWithUrl(final String text, String url) {
        new WebUtils.UrlPreviewAsyncTask() {
            @Override
            protected void onPostExecute(UrlPreviewInfo info) {
                UserMessage tempUserMessage = null;
                BaseChannel.SendUserMessageHandler handler = new BaseChannel.SendUserMessageHandler() {
                    @Override
                    public void onSent(UserMessage userMessage, SendBirdException e) {
                        if (e != null) {
                            // Error!
                            Log.e(LOG_TAG, e.toString());
                            mChatAdapter.markMessageFailed(userMessage.getRequestId());
                            return;
                        }

                        // Update a sent message to RecyclerView
                        mChatAdapter.markMessageSent(new SendBirdMessage(userMessage));
                    }
                };

                try {
                    // Sending a message with URL preview information and custom type.
                    String jsonString = info.toJsonString();
                    tempUserMessage = mChannel.sendUserMessage(text, jsonString, SendBirdChatAdapter.URL_PREVIEW_CUSTOM_TYPE, handler);
                } catch (Exception e) {
                    // Sending a message without URL preview information.
                    tempUserMessage = mChannel.sendUserMessage(text, handler);
                }


                // Display a user message to RecyclerView
                mChatAdapter.addFirst(tempUserMessage);
            }
        }.execute(url);
    }

    private void sendUserMessage(String text) {
        List<String> urls = WebUtils.extractUrls(text);
        if (urls.size() > 0) {
            sendUserMessageWithUrl(text, urls.get(0));
            return;
        }
        if (mChannel != null) {
            UserMessage tempUserMessage = mChannel.sendUserMessage(text, new BaseChannel.SendUserMessageHandler() {
                @Override
                public void onSent(UserMessage userMessage, SendBirdException e) {
                    if (e != null) {
                        // Error!
                        Log.e(LOG_TAG, e.toString());
                        if (e.getCode() == channel_frozen_key) {
                            getActivity().finish();
                        }
                        mChatAdapter.markMessageFailed(userMessage.getRequestId());
                        return;
                    }

                    // Update a sent message to RecyclerView
                    mChatAdapter.markMessageSent(new SendBirdMessage(userMessage));
                }
            });

            // Display a user message to RecyclerView
            mChatAdapter.addFirst(tempUserMessage);
        }
    }

    /**
     * Notify other users whether the current user is typing.
     *
     * @param typing Whether the user is currently typing.
     */
    private void setTypingStatus(boolean typing) {
        if (mChannel == null) {
            return;
        }

        if (typing) {
            mChannel.startTyping();
        } else {
            mChannel.endTyping();
        }
    }

    /**
     * Sends a File Message containing an image file.
     * Also requests thumbnails to be generated in specified sizes.
     *
     * @param uri The URI of the image, which in this case is received through an Intent request.
     */
    private void sendFileWithThumbnail(Uri uri) {
        // Specify two dimensions of thumbnails to generate
        List<FileMessage.ThumbnailSize> thumbnailSizes = new ArrayList<>();
        thumbnailSizes.add(new FileMessage.ThumbnailSize(240, 240));
        thumbnailSizes.add(new FileMessage.ThumbnailSize(320, 320));

        Hashtable<String, Object> info = FileUtils.getFileInfo(getActivity(), uri);

        if (info == null) {
            Toast.makeText(getActivity(), getString(R.string.extracting_file_information_failed), Toast.LENGTH_LONG).show();
            return;
        }

        final String path = (String) info.get("path");
        final File file = new File(path);
        final String name = file.getName();
        final String mime = (String) info.get("mime");
        final int size = (Integer) info.get("size");

        if (path.equals("")) {
            Toast.makeText(getActivity(), getString(R.string.file_must_be_in_local_storage), Toast.LENGTH_LONG).show();
        } else {
            BaseChannel.SendFileMessageWithProgressHandler progressHandler = new BaseChannel.SendFileMessageWithProgressHandler() {
                @Override
                public void onProgress(int bytesSent, int totalBytesSent, int totalBytesToSend) {
                    if (getActivity() != null && isVisible()) {
                        FileMessage fileMessage = mFileProgressHandlerMap.get(this);
                        if (fileMessage != null && totalBytesToSend > 0) {
                            inProgressMessages.put(fileMessage.getRequestId(), false);
                            int percent = (totalBytesSent * 100) / totalBytesToSend;
                            mChatAdapter.setFileProgressPercent(fileMessage, percent);
                        }
                    }
                }

                @Override
                public void onSent(FileMessage fileMessage, SendBirdException e) {
                    try {
                        inProgressMessages.remove(fileMessage.getRequestId());
                    }
                    catch (Exception activity_is_killed) {}
                    if (getActivity() != null && isVisible()) {
                        if (e != null) {
                            Toast.makeText(getActivity(), "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            mChatAdapter.markMessageFailed(fileMessage.getRequestId());
                            return;
                        }
                        mChatAdapter.markMessageSent(new SendBirdMessage(fileMessage));
                    }
                }
            };

            if (mChannel != null) {
                // Send image with thumbnails in the specified dimensions
                FileMessage tempFileMessage = mChannel.sendFileMessage(file, name, mime, size, "", null, thumbnailSizes, progressHandler);

                mFileProgressHandlerMap.put(progressHandler, tempFileMessage);

                mChatAdapter.addTempFileMessageInfo(tempFileMessage, uri);
                mChatAdapter.addFirst(tempFileMessage);
            }
        }
    }

    private void editMessage(final BaseMessage message, String editedMessage) {
        mChannel.updateUserMessage(message.getMessageId(), editedMessage, null, null, new BaseChannel.UpdateUserMessageHandler() {
            @Override
            public void onUpdated(UserMessage userMessage, SendBirdException e) {
                if (e != null) {
                    // Error!
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                mChatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        mChatAdapter.markAllMessagesAsRead();
                    }
                });
            }
        });
    }

    /**
     * Deletes a message within the channel.
     * Note that users can only delete messages sent by oneself.
     *
     * @param message The message to delete.
     */
    private void deleteMessage(final BaseMessage message) {
        mChannel.deleteMessage(message, new BaseChannel.DeleteMessageHandler() {
            @Override
            public void onResult(SendBirdException e) {
                if (e != null) {
                    // Error!
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                mChatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        mChatAdapter.markAllMessagesAsRead();
                    }
                });
            }
        });
    }

    /**
     *
     * @param message
     */
    private void showOtherChannelMessageSnack(String message) {
        TSnackbar snackbar = TSnackbar.make(getActivity().findViewById(android.R.id.content), message, TSnackbar.LENGTH_LONG);
        snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.message_center_primary));
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);
        TextView textView = (TextView) snackbarView.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.message_center_primary));
        snackbar.show();
    }

    private void showInProgressMessageAlert() {
        try {
            new AlertDialog.Builder(getContext()).setMessage(getString(R.string.ms_message_file_in_progress))
                    .setNegativeButton(getString(R.string.ms_message_failed_no), null)
                    .setPositiveButton(getString(R.string.ms_message_failed_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (inProgressMessages != null) {
                                inProgressMessages.clear();
                            }
                            getActivity().onBackPressed();
                        }
                    })
                    .create().show();
        }
        catch (Exception e) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED && requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE) {
            Snackbar.make(mRootLayout, getString(R.string.storage_access_permission_needed), Snackbar.LENGTH_LONG).show();
        }
        else if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED && requestCode == PERMISSION_CAMERA) {
            Snackbar.make(mRootLayout, getString(R.string.camera_access_permission_needed), Snackbar.LENGTH_LONG).show();
        }
    }
}
