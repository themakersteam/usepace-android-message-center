package com.usepace.android.messagingcenter.screens.sendbird;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dinuscxj.progressbar.CircleProgressBar;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.model.SendBirdMessage;
import com.usepace.android.messagingcenter.utils.DateUtils;
import com.usepace.android.messagingcenter.utils.FileUtils;
import com.usepace.android.messagingcenter.utils.ImageUtils;
import com.usepace.android.messagingcenter.utils.TextUtils;
import com.usepace.android.messagingcenter.utils.UrlPreviewInfo;
import com.usepace.android.messagingcenter.utils.WebUtils;

import org.json.JSONException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


class SendBirdChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String URL_PREVIEW_CUSTOM_TYPE = "url_preview";

    private static final int VIEW_TYPE_USER_MESSAGE_ME = 10;
    private static final int VIEW_TYPE_USER_MESSAGE_OTHER = 11;
    private static final int VIEW_TYPE_FILE_MESSAGE_ME = 20;
    private static final int VIEW_TYPE_FILE_MESSAGE_OTHER = 21;
    private static final int VIEW_TYPE_FILE_MESSAGE_IMAGE_ME = 22;
    private static final int VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER = 23;
    private static final int VIEW_TYPE_FILE_MESSAGE_VIDEO_ME = 24;
    private static final int VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER = 25;
    private static final int VIEW_TYPE_ADMIN_MESSAGE = 30;

    private Context mContext;
    private HashMap<FileMessage, CircleProgressBar> mFileMessageMap;
    private GroupChannel mChannel;
    private List<SendBirdMessage> mMessageList;

    private OnItemClickListener mItemClickListener;
    private OnItemLongClickListener mItemLongClickListener;

    private ArrayList<String> mFailedMessageIdList = new ArrayList<>();
    private Hashtable<String, Uri> mTempFileMessageUriTable = new Hashtable<>();
    private boolean mIsMessageListLoading;

    public interface OnItemLongClickListener {
        void onUserMessageItemLongClick(UserMessage message, int position);

        void onFileMessageItemLongClick(FileMessage message);

        void onAdminMessageItemLongClick(AdminMessage message);
    }

    public interface OnItemClickListener {
        void onUserMessageItemClick(UserMessage message);

        void onFileMessageItemClick(FileMessage message);
    }


    public SendBirdChatAdapter(Context context, Bundle args) {
        mContext = context;
        mFileMessageMap = new HashMap<>();
        mMessageList = new ArrayList<>();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public boolean load(String channelUrl) {
        try {
            File appDir = new File(mContext.getCacheDir(), SendBird.getApplicationId());
            appDir.mkdirs();

            File dataFile = new File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().getUserId() + channelUrl) + ".data");

            String content = FileUtils.loadFromFile(dataFile);
            String [] dataArray = content.split("\n");

            mChannel = (GroupChannel) GroupChannel.buildFromSerializedData(Base64.decode(dataArray[0], Base64.DEFAULT | Base64.NO_WRAP));
            // Reset message list, then add cached messages.
            mMessageList.clear();
            for(int i = 1; i < dataArray.length; i++) {
                mMessageList.add(SendBirdMessage.buildFromSerializedData(Base64.decode(dataArray[i], Base64.DEFAULT | Base64.NO_WRAP)));
            }

            notifyDataSetChanged();
            return !mChannel.isFrozen();
        } catch(Exception e) {
            // Nothing to load.
        }
        return true;
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder();
            if (mChannel != null) {
                // Convert current data into string.
                sb.append(Base64.encodeToString(mChannel.serialize(), Base64.DEFAULT | Base64.NO_WRAP));
                SendBirdMessage message = null;
                for (int i = 0; i < Math.min(mMessageList.size(), 100); i++) {
                    message = mMessageList.get(i);
                    if (!isTempMessage(message)) {
                        sb.append("\n");
                        sb.append(Base64.encodeToString(message.serialize(), Base64.DEFAULT | Base64.NO_WRAP));
                    }
                }

                String data = sb.toString();
                String md5 = TextUtils.generateMD5(data);

                // Save the data into file.
                File appDir = new File(mContext.getCacheDir(), SendBird.getApplicationId());
                appDir.mkdirs();

                File hashFile = new File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().getUserId() + mChannel.getUrl()) + ".hash");
                File dataFile = new File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().getUserId() + mChannel.getUrl()) + ".data");

                try {
                    String content = FileUtils.loadFromFile(hashFile);
                    // If data has not been changed, do not save.
                    if(md5.equals(content)) {
                        return;
                    }
                } catch(IOException e) {
                    // File not found. Save the data.
                }

                FileUtils.saveToFile(dataFile, data);
                FileUtils.saveToFile(hashFile, md5);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inflates the correct layout according to the View Type.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                View myUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_user_me, parent, false);
                return new MyUserMessageHolder(myUserMsgView);
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                View otherUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_user_other, parent, false);
                return new OtherUserMessageHolder(otherUserMsgView);
            case VIEW_TYPE_ADMIN_MESSAGE:
                View adminMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_admin, parent, false);
                return new AdminMessageHolder(adminMsgView);
            case VIEW_TYPE_FILE_MESSAGE_ME:
                View myFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_me, parent, false);
                return new MyFileMessageHolder(myFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_OTHER:
                View otherFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_other, parent, false);
                return new OtherFileMessageHolder(otherFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_ME:
                View myImageFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_image_me, parent, false);
                return new MyImageFileMessageHolder(myImageFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER:
                View otherImageFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_image_other, parent, false);
                return new OtherImageFileMessageHolder(otherImageFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_ME:
                View myVideoFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_video_me, parent, false);
                return new MyVideoFileMessageHolder(myVideoFileMsgView);
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER:
                View otherVideoFileMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_file_video_other, parent, false);
                return new OtherVideoFileMessageHolder(otherVideoFileMsgView);

            default:
                return null;

        }
    }

    /**
     * Binds variables in the SendBirdMessage to UI components in the ViewHolder.
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SendBirdMessage message = mMessageList.get(position);
        boolean isContinuous = false;
        boolean isNewDay = false;
        boolean isTempMessage = false;
        boolean isFailedMessage = false;
        Uri tempFileMessageUri = null;

        if (mMessageList.get(position).getBase() != null) {
            // If there is at least one item preceding the current one, check the previous message.
            if (position < mMessageList.size() - 1) {
                SendBirdMessage prevMessage = mMessageList.get(position + 1);

                // If the date of the previous message is different, display the date before the message,
                // and also set isContinuous to false to show information such as the sender's nickname
                // and profile image.
                if (!DateUtils.hasSameDate(message.getCreatedAt(), prevMessage.getCreatedAt())) {
                    isNewDay = true;
                    isContinuous = false;
                } else {
                    isContinuous = isContinuous(message, prevMessage);
                }
            } else if (position == mMessageList.size() - 1) {
                isNewDay = true;
            }
        }

        isTempMessage = isTempMessage(message);
        tempFileMessageUri = getTempFileMessageUri(message);
        isFailedMessage = isFailedMessage(message);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                ((MyUserMessageHolder) holder).bind(mContext, (UserMessage) message.getBase(), mChannel, isContinuous, isNewDay, isTempMessage, isFailedMessage, mItemClickListener, mItemLongClickListener, position);
                break;
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                ((OtherUserMessageHolder) holder).bind(mContext, (UserMessage) message.getBase(), mChannel, isNewDay, isContinuous, mItemClickListener, mItemLongClickListener, position);
                break;
            case VIEW_TYPE_ADMIN_MESSAGE:
                ((AdminMessageHolder) holder).bind(mContext, (AdminMessage) message.getBase(), mChannel, isNewDay);
                break;
            case VIEW_TYPE_FILE_MESSAGE_ME:
                ((MyFileMessageHolder) holder).bind(mContext, (FileMessage) message.getBase(), mChannel, isNewDay, isTempMessage, isFailedMessage, tempFileMessageUri, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_OTHER:
                ((OtherFileMessageHolder) holder).bind(mContext, (FileMessage) message.getBase(), mChannel, isNewDay, isContinuous, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_ME:
                ((MyImageFileMessageHolder) holder).bind(mContext, (FileMessage) message.getBase(), mChannel, isNewDay, isTempMessage, isFailedMessage, tempFileMessageUri, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER:
                ((OtherImageFileMessageHolder) holder).bind(mContext, (FileMessage) message.getBase(), mChannel, isNewDay, isContinuous, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_ME:
                ((MyVideoFileMessageHolder) holder).bind(mContext, (FileMessage) message.getBase(), mChannel, isNewDay, isTempMessage, isFailedMessage, tempFileMessageUri, mItemClickListener);
                break;
            case VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER:
                ((OtherVideoFileMessageHolder) holder).bind(mContext, (FileMessage) message.getBase(), mChannel, isNewDay, isContinuous, mItemClickListener);
                break;
            default:
                break;
        }
    }

    /**
     * Declares the View Type according to the type of message and the sender.
     */
    @Override
    public int getItemViewType(int position) {

        SendBirdMessage message = mMessageList.get(position);
        if (message.getBase() instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message.getBase();
            // If the sender is current user
            if (userMessage.getSender().getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                return VIEW_TYPE_USER_MESSAGE_ME;
            } else {
                return VIEW_TYPE_USER_MESSAGE_OTHER;
            }
        } else if (message.getBase() instanceof FileMessage) {
            FileMessage fileMessage = (FileMessage) message.getBase();
            if (fileMessage.getType().toLowerCase().startsWith("image")) {
                // If the sender is current user
                if (fileMessage.getSender().getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    return VIEW_TYPE_FILE_MESSAGE_IMAGE_ME;
                } else {
                    return VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER;
                }
            } else if (fileMessage.getType().toLowerCase().startsWith("video")) {
                if (fileMessage.getSender().getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    return VIEW_TYPE_FILE_MESSAGE_VIDEO_ME;
                } else {
                    return VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER;
                }
            } else {
                if (fileMessage.getSender().getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    return VIEW_TYPE_FILE_MESSAGE_ME;
                } else {
                    return VIEW_TYPE_FILE_MESSAGE_OTHER;
                }
            }
        } else if (message.getBase() instanceof AdminMessage) {
            return VIEW_TYPE_ADMIN_MESSAGE;
        }

        return -1;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public void setChannel(GroupChannel channel) {
        mChannel = channel;
    }

    public boolean isTempMessage(SendBirdMessage message) {
        return message.getMessageId() == 0;
    }

    public boolean isFailedMessage(SendBirdMessage message) {
        if (!isTempMessage(message)) {
            return false;
        }

        if (message.getBase() instanceof UserMessage) {
            int index = mFailedMessageIdList.indexOf(((UserMessage) message.getBase()).getRequestId());
            if (index >= 0) {
                return true;
            }
        } else if (message.getBase() instanceof FileMessage) {
            int index = mFailedMessageIdList.indexOf(((FileMessage) message.getBase()).getRequestId());
            if (index >= 0) {
                return true;
            }
        }

        return false;
    }


    public Uri getTempFileMessageUri(SendBirdMessage message) {
        if (!isTempMessage(message)) {
            return null;
        }

        if (!(message.getBase() instanceof FileMessage)) {
            return null;
        }

        return mTempFileMessageUriTable.get(((FileMessage) message.getBase()).getRequestId());
    }

    public void markMessageFailed(String requestId) {
        mFailedMessageIdList.add(requestId);
        notifyDataSetChanged();
    }

    public void removeFailedMessage(BaseMessage message) {
        if (message instanceof UserMessage) {
            mFailedMessageIdList.remove(((UserMessage) message).getRequestId());
            mMessageList.remove(message);
        } else if (message instanceof FileMessage) {
            mFailedMessageIdList.remove(((FileMessage) message).getRequestId());
            mTempFileMessageUriTable.remove(((FileMessage) message).getRequestId());
            mMessageList.remove(message);
        }

        notifyDataSetChanged();
    }

    public void setFileProgressPercent(FileMessage message, int percent) {
        SendBirdMessage msg;
        for (int i = mMessageList.size() - 1; i >= 0; i--) {
            msg = mMessageList.get(i);
            if (msg.getBase() instanceof FileMessage) {
                if (message.getRequestId().equals(((FileMessage)msg.getBase()).getRequestId())) {
                    CircleProgressBar circleProgressBar = mFileMessageMap.get(message);
                    if (circleProgressBar != null) {
                        circleProgressBar.setProgress(percent);
                    }
                    break;
                }
            }
        }
    }

    public void markMessageSent(SendBirdMessage message) {
        SendBirdMessage msg;
        for (int i = mMessageList.size() - 1; i >= 0; i--) {
            msg = mMessageList.get(i);
            if (message.getBase() instanceof UserMessage && msg.getBase() instanceof UserMessage) {
                if (((UserMessage) msg.getBase()).getRequestId().equals(((UserMessage) message.getBase()).getRequestId())) {
                    mMessageList.set(i, message);
                    notifyDataSetChanged();
                    return;
                }
            } else if (message.getBase() instanceof FileMessage && msg.getBase() instanceof FileMessage) {
                if (((FileMessage) msg.getBase()).getRequestId().equals(((FileMessage) message.getBase()).getRequestId())) {
                    mTempFileMessageUriTable.remove(((FileMessage) message.getBase()).getRequestId());
                    mMessageList.set(i, message);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void addTempFileMessageInfo(FileMessage message, Uri uri) {
        mTempFileMessageUriTable.put(message.getRequestId(), uri);
    }

    public void addFirst(BaseMessage message) {
        mMessageList.add(0, new SendBirdMessage(message));
        notifyDataSetChanged();
    }

    public void delete(long msgId) {
        for(SendBirdMessage msg : mMessageList) {
            if(msg.getMessageId() == msgId) {
                mMessageList.remove(msg);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void update(BaseMessage message) {
        SendBirdMessage baseMessage;
        for (int index = 0; index < mMessageList.size(); index++) {
            baseMessage = mMessageList.get(index);
            if(message.getMessageId() == baseMessage.getMessageId()) {
                mMessageList.remove(index);
                mMessageList.add(index, new SendBirdMessage(message));
                notifyDataSetChanged();
                break;
            }
        }
    }

    private synchronized boolean isMessageListLoading() {
        return mIsMessageListLoading;
    }

    private synchronized void setMessageListLoading(boolean tf) {
        mIsMessageListLoading = tf;
    }

    /**
     * Notifies that the user has read all (previously unread) messages in the channel.
     * Typically, this would be called immediately after the user enters the chat and loads
     * its most recent messages.
     */
    public void markAllMessagesAsRead() {
        if (mChannel != null) {
            mChannel.markAsRead();
        }
        notifyDataSetChanged();
    }

    /**
     * Load old message list.
     * @param limit
     * @param handler
     */
    public void loadPreviousMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if(isMessageListLoading()) {
            return;
        }

        long oldestMessageCreatedAt = Long.MAX_VALUE;
        if(mMessageList.size() > 0) {
            oldestMessageCreatedAt = mMessageList.get(mMessageList.size() - 1).getCreatedAt();
        }

        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(oldestMessageCreatedAt, false, limit, true, BaseChannel.MessageTypeFilter.ALL, null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if(handler != null) {
                    handler.onResult(list, e);
                }

                setMessageListLoading(false);
                if(e != null) {
                    e.printStackTrace();
                    return;
                }

                for(BaseMessage message : list) {
                    mMessageList.add(new SendBirdMessage(message));
                }

                notifyDataSetChanged();
            }
        });
    }

    /**
     * Replaces current message list with new list.
     * Should be used only on initial load or refresh.
     */
    public void loadLatestMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if(isMessageListLoading()) {
            return;
        }

        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(Long.MAX_VALUE, true, limit, true, BaseChannel.MessageTypeFilter.ALL, null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if(handler != null) {
                    handler.onResult(list, e);
                }

                setMessageListLoading(false);
                if(e != null) {
                    e.printStackTrace();
                    return;
                }

                if(list.size() <= 0) {
                    return;
                }

                for (SendBirdMessage message : mMessageList) {
                    if (isTempMessage(message) || isFailedMessage(message)) {
                        list.add(0, message.getBase());
                    }
                }

                mMessageList.clear();

                for(BaseMessage message : list) {
                    mMessageList.add(new SendBirdMessage(message));
                }

                notifyDataSetChanged();
            }
        });
    }

    public void setItemLongClickListener(OnItemLongClickListener listener) {
        mItemLongClickListener = listener;
    }

    public void setItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    /**
     * Checks if the current message was sent by the same person that sent the preceding message.
     * <p>
     * This is done so that redundant UI, such as sender nickname and profile picture,
     * does not have to displayed when not necessary.
     */
    private boolean isContinuous(SendBirdMessage currentMsg, SendBirdMessage precedingMsg) {
        // null check
        if (currentMsg == null || precedingMsg == null) {
            return false;
        }

        if (currentMsg.getBase() instanceof AdminMessage && precedingMsg.getBase() instanceof AdminMessage) {
            return true;
        }

        User currentUser = null, precedingUser = null;

        if (currentMsg.getBase() instanceof UserMessage) {
            currentUser = ((UserMessage) currentMsg.getBase()).getSender();
        } else if (currentMsg.getBase() instanceof FileMessage) {
            currentUser = ((FileMessage) currentMsg.getBase()).getSender();
        }

        if (precedingMsg.getBase() instanceof UserMessage) {
            precedingUser = ((UserMessage) precedingMsg.getBase()).getSender();
        } else if (precedingMsg.getBase() instanceof FileMessage) {
            precedingUser = ((FileMessage) precedingMsg.getBase()).getSender();
        }

        // If admin message or
        return !(currentUser == null || precedingUser == null)
                && currentUser.getUserId().equals(precedingUser.getUserId());


    }

    /**
     *
     * @param readReceiptImg
     * @param isFailedMessage
     * @param isTempMessage
     */
    private void processReadReceipt(ImageView readReceiptImg, TextView chatTime, boolean isFailedMessage, boolean isTempMessage, GroupChannel channel, UserMessage message) {
        if (isFailedMessage) {
            readReceiptImg.setImageResource(R.drawable.ic_sendfail);
            readReceiptImg.setColorFilter(Color.parseColor("#FFDD2C00"));
            chatTime.setTextColor(Color.parseColor("#fb2b2b"));
            chatTime.setText(mContext.getString(R.string.ms_chat_failed_to_send));
        }
        else if (isTempMessage) {
            readReceiptImg.setImageResource(R.drawable.ic_msgsent);
            readReceiptImg.setColorFilter(Color.parseColor("#9b9b9b"));
        } else {
            // Since setChannel is set slightly after adapter is created
            if (channel != null) {
                int readReceipt = channel.getReadReceipt(message);
                if (readReceipt > 0) {
                    readReceiptImg.setImageResource(R.drawable.ic_msgdelivered);
                    readReceiptImg.setColorFilter(Color.parseColor("#9b9b9b"));
                } else {
                    readReceiptImg.setImageResource(R.drawable.ic_msgdelivered);
                    readReceiptImg.setColorFilter(Color.parseColor("#00c269"));
                }
            }
        }
    }

    /**
     *
     * @param circleProgressBar
     * @param readReceiptImg
     * @param isFailedMessage
     * @param isTempMessage
     * @param channel
     * @param message
     */
    private void processReadReceiptForFile(CircleProgressBar circleProgressBar, ImageView readReceiptImg, TextView chat_time,  boolean isFailedMessage, boolean isTempMessage, GroupChannel channel, FileMessage message) {
        if (isFailedMessage) {
            readReceiptImg.setImageResource(R.drawable.ic_sendfail);
            circleProgressBar.setVisibility(View.GONE);
            mFileMessageMap.remove(message);
            chat_time.setTextColor(Color.parseColor("#fb2b2b"));
            chat_time.setText(mContext.getString(R.string.ms_chat_failed_to_send));
        }
        else if (isTempMessage) {
            readReceiptImg.setImageResource(R.drawable.ic_msgsent);
            readReceiptImg.setColorFilter(Color.parseColor("#9b9b9b"));
            circleProgressBar.setVisibility(View.VISIBLE);
            mFileMessageMap.put(message, circleProgressBar);
        } else {
            circleProgressBar.setVisibility(View.GONE);
            mFileMessageMap.remove(message);
            // Since setChannel is set slightly after adapter is created
            if (channel != null) {
                int readReceipt = channel.getReadReceipt(message);
                if (readReceipt > 0) {
                    readReceiptImg.setImageResource(R.drawable.ic_msgdelivered);
                    readReceiptImg.setColorFilter(Color.parseColor("#9b9b9b"));
                } else {
                    readReceiptImg.setImageResource(R.drawable.ic_msgdelivered);
                    readReceiptImg.setColorFilter(Color.parseColor("#00c269"));
                }
            }
        }
    }

    private class AdminMessageHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        AdminMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_group_chat_message);
        }

        void bind(Context context, AdminMessage message, GroupChannel channel, boolean isNewDay) {
            messageText.setText(message.getMessage());
        }
    }

    private class MyUserMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, editedText, timeText;
        ImageView readReceipt;
        ViewGroup urlPreviewContainer;
        ImageView urlPreviewMainImageView;
        View padding;

        MyUserMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_group_chat_message);
            editedText = (TextView) itemView.findViewById(R.id.text_group_chat_edited);
            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            readReceipt = (ImageView) itemView.findViewById(R.id.img_group_chat_read_receipt);

            urlPreviewContainer = (ViewGroup) itemView.findViewById(R.id.url_preview_container);
            urlPreviewMainImageView = (ImageView) itemView.findViewById(R.id.image_url_preview_main);

            // Dynamic padding that can be hidden or shown based on whether the message is continuous.
            padding = itemView.findViewById(R.id.view_group_chat_padding);
        }

        void bind(final Context context, final UserMessage message, GroupChannel channel, boolean isContinuous, boolean isNewDay, boolean isTempMessage, boolean isFailedMessage, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener, final int position) {
            String mMessage = TextUtils.getLocationUrlMessageIfExists(message.getMessage());
            messageText.setText(mMessage);
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));
            timeText.setTextColor(Color.parseColor("#9b9b9b"));

            if (message.getUpdatedAt() > 0) {
                editedText.setVisibility(View.VISIBLE);
            } else {
                editedText.setVisibility(View.GONE);
            }

            processReadReceipt(readReceipt, timeText, isFailedMessage, isTempMessage, channel, message);

            // If continuous from previous message, remove extra padding.
            if (isContinuous) {
                padding.setVisibility(View.GONE);
            } else {
                padding.setVisibility(View.VISIBLE);
            }

            urlPreviewContainer.setVisibility(View.GONE);
            if (message.getCustomType().equals(URL_PREVIEW_CUSTOM_TYPE)) {
                try {
                    final UrlPreviewInfo previewInfo = new UrlPreviewInfo(message.getData());
                    if (previewInfo.getImageUrl() != null) {
                        urlPreviewContainer.setVisibility(View.VISIBLE);
                        ImageUtils.displayImageFromUrl(context, previewInfo.getImageUrl(), urlPreviewMainImageView, null);
                    } else {
                        urlPreviewContainer.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    urlPreviewContainer.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }
            else if (message.getMessage().startsWith("location://")){
                try {
                    //String locationUrl = getLocationUrl(context, message.getMessage()); //todo : enable when location is static map
                    //ImageUtils.displayImageFromUrl(context, locationUrl, urlPreviewMainImageView, null); //todo : enable when location is static map
                    urlPreviewContainer.setVisibility(View.VISIBLE);
                    Glide.with(context).load(R.drawable.map_place_holder).into(urlPreviewMainImageView);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                urlPreviewContainer.setVisibility(View.GONE);
            }

            if (clickListener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickListener.onUserMessageItemClick(message);
                    }
                });
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        longClickListener.onUserMessageItemLongClick(message, position);
                        return true;
                    }
                });
            }
        }

    }

    private class OtherUserMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, editedText, timeText;

        ViewGroup urlPreviewContainer;
        ImageView urlPreviewMainImageView;

        public OtherUserMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_group_chat_message);
            editedText = (TextView) itemView.findViewById(R.id.text_group_chat_edited);
            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);

            urlPreviewContainer = (ViewGroup) itemView.findViewById(R.id.url_preview_container);
            urlPreviewMainImageView = (ImageView) itemView.findViewById(R.id.image_url_preview_main);
        }


        void bind(final Context context, final UserMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener, final int position) {
            String mMessage = TextUtils.getLocationUrlMessageIfExists(message.getMessage());
            messageText.setText(mMessage);
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            if (message.getUpdatedAt() > 0) {
                editedText.setVisibility(View.VISIBLE);
            } else {
                editedText.setVisibility(View.GONE);
            }

            urlPreviewContainer.setVisibility(View.GONE);
            if (message.getCustomType().equals(URL_PREVIEW_CUSTOM_TYPE)) {
                try {
                    UrlPreviewInfo previewInfo = new UrlPreviewInfo(message.getData());
                    if (previewInfo.getImageUrl() != null) {
                        urlPreviewContainer.setVisibility(View.VISIBLE);
                        ImageUtils.displayImageFromUrl(context, previewInfo.getImageUrl(), urlPreviewMainImageView, null);
                    } else {
                        urlPreviewContainer.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    urlPreviewContainer.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }
            else if (message.getMessage().startsWith("location://")){

                try {
                    //String locationUrl = getLocationUrl(context, message.getMessage()); //todo : enable when location is static map
                    //ImageUtils.displayImageFromUrl(context, locationUrl, urlPreviewMainImageView, null); //todo : enable when location is static map
                    urlPreviewContainer.setVisibility(View.VISIBLE);
                    Glide.with(context).load(R.drawable.map_place_holder).into(urlPreviewMainImageView);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                urlPreviewContainer.setVisibility(View.GONE);
            }

            if (clickListener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickListener.onUserMessageItemClick(message);
                    }
                });
            }
            if (longClickListener != null) {
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        longClickListener.onUserMessageItemLongClick(message, position);
                        return true;
                    }
                });
            }
        }
    }

    private String getLocationUrl(Context context, String message) {
        String locationUrl = "";
        try {
            String[] latLng = message.split("=");
            String[] latStr = latLng[1].split("&");

            double lat = Double.parseDouble(latStr[0]);
            double lng = Double.parseDouble(latLng[2]);

            locationUrl = "https://maps.googleapis.com/maps/api/staticmap?center="+lat + "," + lng
                    +"&zoom=18&size=650x450&maptype=roadmap" + "&markers=color:red%7C"+lat + "," + lng+
                    "&key="+context.getString(R.string.google_maps_key);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return locationUrl;
    }

    private class MyFileMessageHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, timeText;
        CircleProgressBar circleProgressBar;
        ImageView readReceipt;

        public MyFileMessageHolder(View itemView) {
            super(itemView);

            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            fileNameText = (TextView) itemView.findViewById(R.id.text_group_chat_file_name);
            readReceipt = (ImageView) itemView.findViewById(R.id.img_group_chat_read_receipt);
            circleProgressBar = (CircleProgressBar) itemView.findViewById(R.id.circle_progress);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isTempMessage, boolean isFailedMessage, Uri tempFileMessageUri, final OnItemClickListener listener) {
            fileNameText.setText(message.getName());
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));
            timeText.setTextColor(Color.parseColor("#9b9b9b"));

            processReadReceiptForFile(circleProgressBar, readReceipt, timeText, isFailedMessage, isTempMessage, channel, message);

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }

    private class OtherFileMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText, fileNameText;

        public OtherFileMessageHolder(View itemView) {
            super(itemView);

            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            fileNameText = (TextView) itemView.findViewById(R.id.text_group_chat_file_name);

        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener listener) {
            fileNameText.setText(message.getName());
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }

    /**
     * A ViewHolder for file messages that are images.
     * Displays only the image thumbnail.
     */
    private class MyImageFileMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        ImageView fileThumbnailImage, readReceipt;
        CircleProgressBar circleProgressBar;

        public MyImageFileMessageHolder(View itemView) {
            super(itemView);

            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            fileThumbnailImage = (ImageView) itemView.findViewById(R.id.image_group_chat_file_thumbnail);
            readReceipt = (ImageView) itemView.findViewById(R.id.img_group_chat_read_receipt);
            circleProgressBar = (CircleProgressBar) itemView.findViewById(R.id.circle_progress);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isTempMessage, boolean isFailedMessage, Uri tempFileMessageUri, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));
            timeText.setTextColor(Color.parseColor("#9b9b9b"));

            processReadReceiptForFile(circleProgressBar, readReceipt, timeText, isFailedMessage, isTempMessage, channel, message);

            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayImageFromUrl(context, tempFileMessageUri.toString(), fileThumbnailImage, null);
            } else {
                // Get thumbnails from FileMessage
                ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size() > 0) {
                    if (message.getType().toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, thumbnails.get(0).getUrl(), fileThumbnailImage.getDrawable());
                    } else {
                        ImageUtils.displayImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage, fileThumbnailImage.getDrawable());
                    }
                } else {
                    if (message.getType().toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, (String) null, fileThumbnailImage.getDrawable());
                    } else {
                        ImageUtils.displayImageFromUrl(context, message.getUrl(), fileThumbnailImage, fileThumbnailImage.getDrawable());
                    }
                }
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }

    private class OtherImageFileMessageHolder extends RecyclerView.ViewHolder {

        TextView timeText;
        ImageView fileThumbnailImage;

        public OtherImageFileMessageHolder(View itemView) {
            super(itemView);

            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            fileThumbnailImage = (ImageView) itemView.findViewById(R.id.image_group_chat_file_thumbnail);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Get thumbnails from FileMessage
            ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size() > 0) {
                if (message.getType().toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, thumbnails.get(0).getUrl(), fileThumbnailImage.getDrawable());
                } else {
                    ImageUtils.displayImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage, fileThumbnailImage.getDrawable());
                }
            } else {
                if (message.getType().toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(context, message.getUrl(), fileThumbnailImage, (String) null, fileThumbnailImage.getDrawable());
                } else {
                    ImageUtils.displayImageFromUrl(context, message.getUrl(), fileThumbnailImage, fileThumbnailImage.getDrawable());
                }
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }

    /**
     * A ViewHolder for file messages that are videos.
     * Displays only the video thumbnail.
     */
    private class MyVideoFileMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        ImageView fileThumbnailImage, readReceiptText;
        CircleProgressBar circleProgressBar;

        public MyVideoFileMessageHolder(View itemView) {
            super(itemView);

            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            fileThumbnailImage = (ImageView) itemView.findViewById(R.id.image_group_chat_file_thumbnail);
            readReceiptText = (ImageView) itemView.findViewById(R.id.img_group_chat_read_receipt);
            circleProgressBar = (CircleProgressBar) itemView.findViewById(R.id.circle_progress);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isTempMessage, boolean isFailedMessage, Uri tempFileMessageUri, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));
            timeText.setTextColor(Color.parseColor("#9b9b9b"));

            processReadReceiptForFile(circleProgressBar, readReceiptText, timeText,  isFailedMessage, isTempMessage, channel, message);

            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayImageFromUrl(context, tempFileMessageUri.toString(), fileThumbnailImage, null);
            } else {
                // Get thumbnails from FileMessage
                ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size() > 0) {
                    ImageUtils.displayImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage, fileThumbnailImage.getDrawable());
                }
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }

    private class OtherVideoFileMessageHolder extends RecyclerView.ViewHolder {

        TextView timeText;
        ImageView  fileThumbnailImage;

        public OtherVideoFileMessageHolder(View itemView) {
            super(itemView);

            timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
            fileThumbnailImage = (ImageView) itemView.findViewById(R.id.image_group_chat_file_thumbnail);
        }

        void bind(Context context, final FileMessage message, GroupChannel channel, boolean isNewDay, boolean isContinuous, final OnItemClickListener listener) {
            timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

            // Get thumbnails from FileMessage
            ArrayList<FileMessage.Thumbnail> thumbnails = (ArrayList<FileMessage.Thumbnail>) message.getThumbnails();

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size() > 0) {
                ImageUtils.displayImageFromUrl(context, thumbnails.get(0).getUrl(), fileThumbnailImage, fileThumbnailImage.getDrawable());
            }

            if (listener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileMessageItemClick(message);
                    }
                });
            }
        }
    }
}
