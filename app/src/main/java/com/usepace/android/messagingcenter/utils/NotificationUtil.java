package com.usepace.android.messagingcenter.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import java.util.List;

public class NotificationUtil {

    private int notification_id = 2343323;
    private String channel_id = "channel001";

    /**
     *
     * @param context
     * @param intent
     * @param icon
     * @param title
     * @param message
     * @param messages
     */
    public void generateOne(Context context, Intent intent, int icon, String title, String message, List<String> messages) {
        registerChannel(context);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(context, channel_id)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)   // heads-up
                .setContentIntent(pendingIntent)
                .setStyle(compactStyle(messages))
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notification_id, notification);
    }

    /**
     *
     * @param messages
     * @return
     */
    private NotificationCompat.InboxStyle compactStyle(List<String> messages) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        if (messages != null) {
            int more = 0;
            for (int i = 0; i < messages.size(); i++) {
                if (i < 2) {
                    style.addLine(messages.get(i));
                }
                else {
                    more ++;
                }
            }
            if (more > 0) {
                style.setSummaryText("+" + more + " .....");
            }
        }
        return style;
    }

    /**
     *
     * @param context
     */
    private void registerChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(channel_id, "MESSAGE_CENTER", NotificationManager.IMPORTANCE_HIGH);   // for heads-up notifications
            channel.setDescription("heads-up notification channel");
            // Register channel with system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
