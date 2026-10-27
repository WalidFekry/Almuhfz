package com.walid.almuhfz.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.walid.almuhfz.MainActivity;
import com.walid.almuhfz.R;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        sendReminderNotification();
        return Result.success();
    }

    private void sendReminderNotification() {
        Context context = getApplicationContext();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ReminderManager.CHANNEL_ID,
                    "تذكير الحفظ اليومي",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("إشعارات تذكير لورد الحفظ والمراجعة اليومي");
            notificationManager.createNotificationChannel(channel);
        }

        int msgIdx = ReminderManager.getSavedMessageIndex(context);
        String title = (msgIdx >= 0 && msgIdx < ReminderManager.MESSAGES_TITLES.length)
                ? ReminderManager.MESSAGES_TITLES[msgIdx]
                : ReminderManager.MESSAGES_TITLES[0];

        String body = (msgIdx >= 0 && msgIdx < ReminderManager.MESSAGES_BODIES.length)
                ? ReminderManager.MESSAGES_BODIES[msgIdx]
                : ReminderManager.MESSAGES_BODIES[0];

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                ReminderManager.NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        notificationManager.notify(ReminderManager.NOTIFICATION_ID, builder.build());
    }
}
