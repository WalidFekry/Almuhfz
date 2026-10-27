package com.walid.almuhfz.notifications_messages;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.NoInternetActivity;
import com.walid.almuhfz.R;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.notifications_messages.adapter.NotificationAdapter;
import com.walid.almuhfz.notifications_messages.db.SavedNotificationEntity;
import com.walid.almuhfz.notifications_messages.model.NotificationModel;
import com.walid.almuhfz.notifications_messages.model.NotificationResponse;
import com.walid.almuhfz.notifications_messages.repository.NotificationRepository;
import com.walid.almuhfz.utils.AppExecutors;
import com.walid.almuhfz.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class NotificationsMessagesActivity extends AppCompatActivity {
    private static final String TAG = "NotificationsMessages";
    private RecyclerView rec;
    private NotificationAdapter adapter;
    private Dialog loadingDialog;

    private final NotificationRepository repository = new NotificationRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_messages);
        initViews();
        setupRecyclerView();
        showLoading();
        fetchNotificationsMessages();
    }

    private void showLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }
        if (isFinishing() || isDestroyed()) {
            return;
        }

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void hideLoading() {
        if (isFinishing() || isDestroyed()) {
            if (loadingDialog != null) {
                try {
                    loadingDialog.dismiss();
                } catch (Exception ignored) {
                }
                loadingDialog = null;
            }
            return;
        }

        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog", e);
            }
            loadingDialog = null;
        }
    }

    private void initViews() {
        AppCompatImageButton back = findViewById(R.id.back_button);
        rec = findViewById(R.id.rec);
        back.setOnClickListener(v -> finish());

        View btnSavedMessages = findViewById(R.id.btnSavedMessages);
        if (btnSavedMessages != null) {
            btnSavedMessages.setOnClickListener(v -> startActivity(new Intent(this, SavedNotificationsActivity.class)));
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        rec.setLayoutManager(layoutManager);
        rec.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation));
    }

    private void fetchNotificationsMessages() {
        repository.fetchNotifications(new NotificationRepository.OnNotificationsFetchedListener() {
            @Override
            public void onSuccess(NotificationResponse notifications) {
                if (isFinishing() || isDestroyed()) return;

                List<NotificationModel> posts = (notifications != null && notifications.getPosts() != null)
                        ? notifications.getPosts() : new ArrayList<>();
                updateRecyclerView(posts);
                Log.d(TAG, "Fetched " + posts.size() + " notifications");
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                hideLoading();
                Log.e(TAG, "Error: " + errorMessage);
                loadSavedNotificationsFallback();
            }
        });
    }

    private void loadSavedNotificationsFallback() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<SavedNotificationEntity> entities = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAllSavedNotifications();
            List<NotificationModel> fallbackModels = new ArrayList<>();
            if (entities != null) {
                for (SavedNotificationEntity entity : entities) {
                    if (entity != null) {
                        fallbackModels.add(new NotificationModel(entity.getId(), entity.getTitle(), entity.getCreatedAt()));
                    }
                }
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (!fallbackModels.isEmpty()) {
                    updateRecyclerView(fallbackModels);
                    Toast.makeText(this, "تم عرض الرسائل المحفوظة (وضع الأوفلاين) 🔖", Toast.LENGTH_LONG).show();
                } else {
                    openNoInternet();
                }
            });
        });
    }

    private void openNoInternet() {
        Intent i = new Intent(this, NoInternetActivity.class);
        startActivity(i);
        finish();
    }

    private void updateRecyclerView(List<NotificationModel> notifications) {
        if (isFinishing() || isDestroyed()) return;

        adapter = new NotificationAdapter(notifications);
        rec.setAdapter(adapter);
        hideLoading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repository.cancelActiveCall();
        hideLoading();
    }
}
