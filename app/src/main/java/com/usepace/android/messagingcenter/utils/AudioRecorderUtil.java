package com.usepace.android.messagingcenter.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by mohammednabil on 2019-07-29.
 */
public class AudioRecorderUtil {

    private static AudioRecorderUtil audioRecorderUtil = null;
    private MediaRecorder recorder = null;
    private String fileName = null;

    public void startRecording(Context context) {
        fileName = context.getExternalCacheDir().getAbsolutePath() + "record_" + System.currentTimeMillis() + ".wav";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("AUDIO_RECORDER", "prepare() failed");
        }
        recorder.start();
    }

    public void cancelRecording() {
        stopRecording();
        if (recorder != null && fileName != null) {
            File file = new File(fileName);
            file.delete();
        }
    }

    public String stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            return fileName;
        }
        return null;
    }


    public static AudioRecorderUtil instance() {
        if (audioRecorderUtil == null)
            audioRecorderUtil = new AudioRecorderUtil();
        return audioRecorderUtil;
    }
}
