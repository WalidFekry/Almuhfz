package com.walid.almuhfz.reminder;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.walid.almuhfz.MainActivity;
import com.walid.almuhfz.R;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class ReminderManager {

    private static final String TAG = "ReminderManager";
    public static final String WORK_NAME = "QuranDailyReminderWork";
    public static final String PREF_NAME = "reminder_prefs";

    public static final String KEY_ENABLED = "key_reminder_enabled";
    public static final String KEY_HOUR = "key_reminder_hour";
    public static final String KEY_MINUTE = "key_reminder_minute";
    public static final String KEY_MESSAGE_INDEX = "key_reminder_msg_index";

    public static final String CHANNEL_ID = "quran_daily_reminder_channel";
    public static final int NOTIFICATION_ID = 1002;

    public static final String[] MESSAGES_TITLES = new String[]{
            "ورد القرآن الكريم 📖",
            "نور قلبك بالقرآن 🌟",
            "لا تهجر المصحف 🌿"
    };

    public static final String[] MESSAGES_BODIES = new String[]{
            "حان موعد وردك اليومي لمراجعة وحفظ القرآن الكريم",
            "قال النبي ﷺ: اقرءوا القرآن فإنه يأتي يوم القيامة شفيعاً لأصحابه.",
            "جدّد عهدك مع كتاب الله اليوم وابدأ بقراءة صفحة واحدة على الأقل."
    };

    public static void scheduleDailyReminder(Context context, int hourOfDay, int minute, int msgIndex) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putInt(KEY_HOUR, hourOfDay)
                .putInt(KEY_MINUTE, minute)
                .putInt(KEY_MESSAGE_INDEX, msgIndex)
                .apply();

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hourOfDay);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        long initialDelayMs = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        reminderRequest
                );

        Log.d(TAG, "Scheduled daily reminder for " + hourOfDay + ":" + minute + " in " + (initialDelayMs / 1000) + "s");
    }

    public static void cancelReminder(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, false).apply();

        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Cancelled daily reminder work.");
    }

    public static void sendInstantTestNotification(Context context) {
        if (context == null) return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "تذكير الحفظ اليومي",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("إشعارات تذكير لورد الحفظ والمراجعة اليومي");
            notificationManager.createNotificationChannel(channel);
        }

        int msgIdx = getSavedMessageIndex(context);
        String title = (msgIdx >= 0 && msgIdx < MESSAGES_TITLES.length) ? MESSAGES_TITLES[msgIdx] : MESSAGES_TITLES[0];
        String body = (msgIdx >= 0 && msgIdx < MESSAGES_BODIES.length) ? MESSAGES_BODIES[msgIdx] : MESSAGES_BODIES[0];

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID + 10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("[اختبار] " + title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID + 10, builder.build());
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (context == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true;
    }

    public static void requestIgnoreBatteryOptimizations(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivity(intent);
                } else {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    activity.startActivity(intent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening battery settings", e);
                try {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    activity.startActivity(intent);
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isReminderEnabled(Context context) {
        if (context == null) return false;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    public static int getSavedHour(Context context) {
        if (context == null) return 20;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_HOUR, 20);
    }

    public static int getSavedMinute(Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_MINUTE, 0);
    }

    public static int getSavedMessageIndex(Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_MESSAGE_INDEX, 0);
    }
}
