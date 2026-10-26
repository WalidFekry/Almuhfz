package com.walid.almuhfz;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.walid.almuhfz.learning.LearningActivity;
import com.walid.almuhfz.notifications_messages.NotificationsMessagesActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final int DAILY_NOTIFICATION_CODE = 1525;
    private static final String CHANNEL_ID = "Remote_notification_fire_base_new";
    private static final String RANDOM_KEY = "OpenName";

    // Whitelisted target activity classes to prevent unsafe intent redirection
    private static final List<Class<?>> ALLOWED_TARGET_ACTIVITIES = Arrays.asList(
            MainActivity.class,
            NotificationsMessagesActivity.class,
            LearningActivity.class
    );

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null) {
            if (title == null || title.isEmpty()) {
                title = data.get("title");
            }
            if (body == null || body.isEmpty()) {
                body = data.get("body");
            }
        }

        if (title == null) {
            title = getString(R.string.app_name);
        }
        if (body == null) {
            body = "";
        }

        Class<?> targetActivity = MainActivity.class;
        if (data != null && data.containsKey(RANDOM_KEY)) {
            String className = data.get(RANDOM_KEY);
            if (className != null && !className.trim().isEmpty()) {
                Class<?> resolvedClass = resolveAllowedClass(className.trim());
                if (resolvedClass != null) {
                    targetActivity = resolvedClass;
                }
            }
        }

        sendNotification(title, body, targetActivity);
    }

    private Class<?> resolveAllowedClass(String className) {
        for (Class<?> clazz : ALLOWED_TARGET_ACTIVITIES) {
            if (clazz.getName().equals(className) || clazz.getSimpleName().equals(className)) {
                return clazz;
            }
        }
        Log.w(TAG, "Requested target class not in whitelist: " + className);
        return null;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Channel";
            String description = "Channel for notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private PendingIntent getPendingIntent(Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(getApplicationContext(), DAILY_NOTIFICATION_CODE, intent, flags);
    }

    private void sendNotification(String title, String messageBody, Class<?> targetClass) {
        Intent intent = new Intent(getApplicationContext(), targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = getPendingIntent(intent);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(DAILY_NOTIFICATION_CODE, notificationBuilder.build());
        }
    }
}
