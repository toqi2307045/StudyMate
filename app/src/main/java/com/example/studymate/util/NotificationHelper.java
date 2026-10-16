package com.example.studymate.util;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.studymate.R;
import com.example.studymate.ui.MainActivity;

public final class NotificationHelper {
    public static final String CH_CLASS = "classes";
    public static final String CH_TASK = "tasks";
    public static final String CH_POMO = "pomodoro";
    public static final String CH_ATT = "attendance";

    private NotificationHelper() {}

    public static void createChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH_CLASS, "Class reminders", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CH_TASK, "Assignment reminders", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CH_POMO, "Study timer", NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(CH_ATT, "Attendance warnings", NotificationManager.IMPORTANCE_DEFAULT));
    }

    @SuppressLint("MissingPermission")
    public static void show(Context c, String channel, int id, String title, String text) {
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException ignored) {
            // notification permission not granted
        }
    }
}
