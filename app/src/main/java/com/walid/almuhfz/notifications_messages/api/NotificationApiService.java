package com.walid.almuhfz.notifications_messages.api;

import com.walid.almuhfz.notifications_messages.model.NotificationModel;
import com.walid.almuhfz.notifications_messages.model.NotificationResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface NotificationApiService {
    @GET("auto_notification/auto_notification_apps/api_post_apps.php")
    Call<NotificationResponse> getAllNotificationsMessages();
}
