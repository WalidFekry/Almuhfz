package com.walid.almuhfz.notifications_messages;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.notifications_messages.adapter.NotificationAdapter;
import com.walid.almuhfz.notifications_messages.db.SavedNotificationEntity;
import com.walid.almuhfz.notifications_messages.model.NotificationModel;
import com.walid.almuhfz.utils.AppExecutors;
import com.walid.almuhfz.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class SavedNotificationsActivity extends AppCompatActivity {

    private RecyclerView savedNotificationsRecyclerView;
    private ProgressBar savedProgressBar;
    private LinearLayout emptyStateContainer;

    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_notifications);

        initViews();
        setupRecyclerView();

        loadSavedNotifications();
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        savedNotificationsRecyclerView = findViewById(R.id.savedNotificationsRecyclerView);
        savedProgressBar = findViewById(R.id.savedProgressBar);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
    }

    private void setupRecyclerView() {
        savedNotificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadSavedNotifications() {
        savedProgressBar.setVisibility(View.VISIBLE);

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<SavedNotificationEntity> entities = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAllSavedNotifications();

            List<NotificationModel> models = new ArrayList<>();
            if (entities != null) {
                for (SavedNotificationEntity entity : entities) {
                    if (entity != null) {
                        models.add(new NotificationModel(entity.getId(), entity.getTitle(), entity.getCreatedAt()));
                    }
                }
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                savedProgressBar.setVisibility(View.GONE);

                if (models.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    savedNotificationsRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                    savedNotificationsRecyclerView.setVisibility(View.VISIBLE);

                    adapter = new NotificationAdapter(models);
                    savedNotificationsRecyclerView.setAdapter(adapter);
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedNotifications();
    }
}
