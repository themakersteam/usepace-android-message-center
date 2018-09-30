package com.usepace.android.messagingcenter.interfaces;

public interface ProgressInterface {
    void onProgress(int bytesSent, int totalBytesSent, int totalBytesToSend);
}
