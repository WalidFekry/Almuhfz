package com.walid.almuhfz.notifications_messages.repository;

import androidx.annotation.NonNull;

import com.walid.almuhfz.notifications_messages.api.NotificationApiService;
import com.walid.almuhfz.notifications_messages.model.NotificationResponse;
import com.walid.almuhfz.notifications_messages.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NotificationRepository {

    private final NotificationApiService apiService;
    private Call<NotificationResponse> activeCall;

    public NotificationRepository() {
        Retrofit retrofit = RetrofitClient.getClient();
        apiService = retrofit.create(NotificationApiService.class);
    }

    public void fetchNotifications(final OnNotificationsFetchedListener listener) {
        cancelActiveCall();

        activeCall = apiService.getAllNotificationsMessages();
        activeCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    listener.onSuccess(response.body());
                } else {
                    listener.onFailure("Response is empty or unsuccessful");
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) return;
                listener.onFailure(t.getMessage());
            }
        });
    }

    public void cancelActiveCall() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
    }

    public interface OnNotificationsFetchedListener {
        void onSuccess(NotificationResponse notifications);
        void onFailure(String errorMessage);
    }
}
