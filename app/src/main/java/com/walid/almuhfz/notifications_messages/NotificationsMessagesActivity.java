package com.walid.almuhfz.notifications_messages;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.notifications_messages.adapter.NotificationAdapter;
import com.walid.almuhfz.notifications_messages.model.NotificationModel;
import com.walid.almuhfz.notifications_messages.model.NotificationResponse;
import com.walid.almuhfz.notifications_messages.repository.NotificationRepository;

import java.util.List;

public class NotificationsMessagesActivity extends AppCompatActivity {
    private static final String TAG = "NotificationsMessages";
    private RecyclerView rec;
    private NotificationAdapter adapter;
    private Dialog loadingDialog;

    private final NotificationRepository repository = new NotificationRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_messages);
        initViews();
        setupRecyclerView();
        showLoading();
        fetchNotificationsMessages();
    }

    private void showLoading() {
        if (loadingDialog != null) {
            hideLoading();
        }
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void initViews() {
        AppCompatImageButton back = findViewById(R.id.back_button);
        rec = findViewById(R.id.rec);
        back.setOnClickListener(v -> finish());
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
                updateRecyclerView(notifications.getPosts());
                Log.d(TAG, "Fetched " + notifications.getPosts().size() + " notifications");
            }

            @Override
            public void onFailure(String errorMessage) {
                hideLoading();
                Toast.makeText(NotificationsMessagesActivity.this, "يبدو أن هناك مشكلة في الاتصال بالإنترنت. يرجى التحقق من اتصالك وحاول مرة أخرى.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error: " + errorMessage);
            }
        });
    }

    private void updateRecyclerView(List<NotificationModel> notifications) {
        if (adapter == null) {
            adapter = new NotificationAdapter(notifications);
            rec.setAdapter(adapter);
        }
      hideLoading();
    }
}
