package com.usepace.android.messagingcenter.utils;


import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.sendbird.android.FileMessage;
import com.usepace.android.messagingcenter.R;

/**
 * Created by mohammednabil on 2019-07-29.
 */
public class AudioPlayerUtils {

    private static MediaPlayer mediaPlayer;
    private static String lastSelectedAudioPath = null;
    private static ImageView lastMediaIv;
    private static SeekBar lastSeekBar;
    private static TextView lastTimer;
    private static Handler handler;

    public static void setup(final Context context, final ImageView mediaButton, final TextView timer, final SeekBar seekBar, final FileMessage message) {
        boolean isPlayingForTag = isPlayingForTag(mediaButton.getTag().toString());
        mediaButton.setImageResource(isPlayingForTag ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
        seekBar.setEnabled(isPlayingForTag);
        if (isPlayingForTag) {
            seekBar.setMax(mediaPlayer.getDuration());
            lastMediaIv = mediaButton;
            lastSeekBar = seekBar;
            lastTimer = timer;
            setupWithHandler(mediaButton, timer, seekBar);
        }
        else {
            seekBar.setOnSeekBarChangeListener(null);
            seekBar.setProgress(0);
        }
        mediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer == null) {
                    setupPlayer(context, message, seekBar);
                    mediaButton.setImageResource(R.drawable.ic_media_pause);
                }
                else if (!lastSelectedAudioPath.equals(mediaButton.getTag().toString())) {
                    mediaPlayer.release();
                    setupPlayer(context, message, seekBar);
                    mediaButton.setImageResource(R.drawable.ic_media_pause);
                    if (lastMediaIv != null)
                        lastMediaIv.setImageResource(R.drawable.ic_media_play);
                    if (lastSeekBar != null) {
                        lastSeekBar.setProgress(0);
                        lastSeekBar.setEnabled(false);
                    }
                    if (lastTimer != null)
                        lastTimer.setText("00:00");
                }
                else if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mediaButton.setImageResource(R.drawable.ic_media_play);
                    seekBar.setEnabled(false);
                }
                else {
                    seekBar.setEnabled(true);
                    mediaButton.setImageResource(R.drawable.ic_media_pause);
                    mediaPlayer.start();
                }
                setupWithHandler(mediaButton, timer, seekBar);

                lastMediaIv = mediaButton;
                lastSeekBar = seekBar;
                lastTimer = timer;
            }
        });
    }

    private static void setupPlayer(Context context, FileMessage message, SeekBar seekBar) {
        mediaPlayer = MediaPlayer.create(context, Uri.parse(message.getUrl()));
        mediaPlayer.start();
        lastSelectedAudioPath = message.getUrl();
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setEnabled(true);

    }

    private static void setupWithHandler(final ImageView mediaButton, final TextView timer,final SeekBar seekBar) {
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying() && mediaButton.getTag().toString().equals(lastSelectedAudioPath)) {
                        timer.setText(DateUtils.formatMediaTime(mediaPlayer.getCurrentPosition()));
                        if (seekBar != null)
                            seekBar.setOnSeekBarChangeListener(null);
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        if (seekBar != null)
                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                    mediaPlayer.seekTo(i);
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {
                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                }
                            });
                        handler.postDelayed(this, 500);
                    }
                }
            }, 500);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaButton.setImageResource(R.drawable.ic_media_play);
                seekBar.setProgress(0);
                timer.setText("0:00");
                seekBar.setEnabled(false);
            }
        });
    }

    private static boolean isPlayingForTag(String tag) {
        return lastSelectedAudioPath != null && mediaPlayer != null && lastSelectedAudioPath.equals(tag);
    }

    public static void destroy() {
        if (mediaPlayer != null)
            mediaPlayer.release();
        mediaPlayer = null;
        lastSelectedAudioPath = null;
        lastMediaIv = null;
        lastSeekBar = null;
        lastTimer = null;
        handler = null;
    }
}
