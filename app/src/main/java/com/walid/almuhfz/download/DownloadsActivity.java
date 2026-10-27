package com.walid.almuhfz.download;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.learning.LearningActivity;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadsActivity extends AppCompatActivity implements DownloadManager.DownloadListener, DownloadsAdapter.ActionListener {

    private enum FilterMode {
        ALL,
        COMPLETED,
        DOWNLOADING,
        FAILED
    }

    private FilterMode currentFilterMode = FilterMode.ALL;

    private TextView tvTotalSpace, tvCompletedCount, tvDownloadingCount, tvFailedCount;
    private Button btnDeleteAll, btnGoMain;
    private TextView chipAll, chipCompleted, chipDownloading, chipFailed;
    private RecyclerView downloadsRecyclerView;
    private ProgressBar downloadsProgressBar;
    private LinearLayout emptyStateContainer;

    private DownloadsAdapter adapter;
    private List<DownloadItem> allScannedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        initViews();
        setupListeners();
        setupRecyclerView();

        DownloadManager.getInstance().addListener(this);

        loadDownloadsData();
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tvTotalSpace = findViewById(R.id.tvTotalSpace);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvDownloadingCount = findViewById(R.id.tvDownloadingCount);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnGoMain = findViewById(R.id.btnGoMain);

        chipAll = findViewById(R.id.chipAll);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipDownloading = findViewById(R.id.chipDownloading);
        chipFailed = findViewById(R.id.chipFailed);

        downloadsRecyclerView = findViewById(R.id.downloadsRecyclerView);
        downloadsProgressBar = findViewById(R.id.downloadsProgressBar);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
    }

    private void setupListeners() {
        btnDeleteAll.setOnClickListener(v -> confirmDeleteAllDownloads());
        btnGoMain.setOnClickListener(v -> finish());

        chipAll.setOnClickListener(v -> setFilterMode(FilterMode.ALL));
        chipCompleted.setOnClickListener(v -> setFilterMode(FilterMode.COMPLETED));
        chipDownloading.setOnClickListener(v -> setFilterMode(FilterMode.DOWNLOADING));
        chipFailed.setOnClickListener(v -> setFilterMode(FilterMode.FAILED));
    }

    private void setupRecyclerView() {
        adapter = new DownloadsAdapter(this);
        downloadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        downloadsRecyclerView.setAdapter(adapter);
    }

    private void setFilterMode(FilterMode mode) {
        this.currentFilterMode = mode;
        updateFilterChipStyles();
        applyCurrentFilter();
    }

    private void updateFilterChipStyles() {
        int primaryBg = ContextCompat.getColor(this, R.color.primary_color);
        int surfaceBg = ContextCompat.getColor(this, R.color.surface_card);
        int textOnPrimary = ContextCompat.getColor(this, R.color.text_on_primary);
        int textPrimary = ContextCompat.getColor(this, R.color.text_primary_color);

        chipAll.setBackgroundTintList(ColorStateList.valueOf(currentFilterMode == FilterMode.ALL ? primaryBg : surfaceBg));
        chipAll.setTextColor(currentFilterMode == FilterMode.ALL ? textOnPrimary : textPrimary);

        chipCompleted.setBackgroundTintList(ColorStateList.valueOf(currentFilterMode == FilterMode.COMPLETED ? primaryBg : surfaceBg));
        chipCompleted.setTextColor(currentFilterMode == FilterMode.COMPLETED ? textOnPrimary : textPrimary);

        chipDownloading.setBackgroundTintList(ColorStateList.valueOf(currentFilterMode == FilterMode.DOWNLOADING ? primaryBg : surfaceBg));
        chipDownloading.setTextColor(currentFilterMode == FilterMode.DOWNLOADING ? textOnPrimary : textPrimary);

        chipFailed.setBackgroundTintList(ColorStateList.valueOf(currentFilterMode == FilterMode.FAILED ? primaryBg : surfaceBg));
        chipFailed.setTextColor(currentFilterMode == FilterMode.FAILED ? textOnPrimary : textPrimary);
    }

    private void loadDownloadsData() {
        downloadsProgressBar.setVisibility(View.VISIBLE);

        DownloadManager.getInstance().scanAllDownloadItems(this, (items, totalCompletedSize, completedCount, inProgressCount, failedCount) -> {
            if (isFinishing() || isDestroyed()) return;

            downloadsProgressBar.setVisibility(View.GONE);
            allScannedItems = (items != null) ? items : new ArrayList<>();

            tvTotalSpace.setText(formatBytes(totalCompletedSize));
            tvCompletedCount.setText("المكتملة: " + completedCount);
            tvDownloadingCount.setText("جاري التحميل: " + inProgressCount);
            tvFailedCount.setText("فشلت/متوقفة: " + failedCount);

            applyCurrentFilter();
        });
    }

    private void applyCurrentFilter() {
        List<DownloadItem> filteredList = new ArrayList<>();

        for (DownloadItem item : allScannedItems) {
            if (currentFilterMode == FilterMode.ALL) {
                filteredList.add(item);
            } else if (currentFilterMode == FilterMode.COMPLETED && item.getState() == DownloadManager.State.COMPLETED) {
                filteredList.add(item);
            } else if (currentFilterMode == FilterMode.DOWNLOADING && item.getState() == DownloadManager.State.DOWNLOADING) {
                filteredList.add(item);
            } else if (currentFilterMode == FilterMode.FAILED && (item.getState() == DownloadManager.State.FAILED || item.getState() == DownloadManager.State.PAUSED)) {
                filteredList.add(item);
            }
        }

        adapter.setItems(filteredList);

        if (filteredList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            downloadsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            downloadsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 MB";
        double sizeInMb = bytes / (1024.0 * 1024.0);
        if (sizeInMb < 0.1) {
            double sizeInKb = bytes / 1024.0;
            return String.format(Locale.US, "%.1f KB", sizeInKb);
        }
        return String.format(Locale.US, "%.1f MB", sizeInMb);
    }

    private void confirmDeleteAllDownloads() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("حذف جميع التنزيلات 🗑️");
        builder.setMessage("هل أنت متأكد من حذف كافة الملفات والتلاوات المحفوظة أوفلاين؟");
        builder.setPositiveButton("حذف الكل", (dialog, which) -> {
            downloadsProgressBar.setVisibility(View.VISIBLE);
            DownloadManager.getInstance().deleteAllDownloadedSurahs(this, () -> {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(this, "تم حذف جميع التنزيلات أوفلاين", Toast.LENGTH_SHORT).show();
                loadDownloadsData();
            });
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    @Override
    public void onPlayOfflineClicked(DownloadItem item) {
        if (item == null || item.getReciterModel() == null || item.getSoraDetailsModel() == null) {
            Toast.makeText(this, "تعذر تشغيل هذه السورة أوفلاين", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, LearningActivity.class);
        LearningData data = new LearningData(
                item.getReciterModel(),
                item.getSoraDetailsModel(),
                item.getSurahNumber(),
                1,
                item.getTotalAyahs() > 0 ? item.getTotalAyahs() : 1,
                1,
                1
        );
        intent.putExtra("learning_data", data);
        startActivity(intent);
    }

    @Override
    public void onResumeRetryClicked(DownloadItem item) {
        if (item == null || item.getReciterModel() == null || item.getSoraDetailsModel() == null) {
            Toast.makeText(this, "بيانات السورة غير مكتملة لاستئناف التحميل", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.getInstance().startOrResumeSurahDownload(this, item.getReciterModel(), item.getSoraDetailsModel());
        Toast.makeText(this, "جاري استئناف التحميل...", Toast.LENGTH_SHORT).show();
        loadDownloadsData();
    }

    @Override
    public void onPauseClicked(DownloadItem item) {
        if (item == null) return;
        DownloadManager.getInstance().pauseOrCancelDownload(item.getSurahNumber(), item.getReciterId());
        Toast.makeText(this, "تم إيقاف التحميل مؤقتاً", Toast.LENGTH_SHORT).show();
        loadDownloadsData();
    }

    @Override
    public void onDeleteClicked(DownloadItem item) {
        if (item == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("حذف التنزيل 🗑️");
        builder.setMessage("هل أنت متأكد من حذف الملفات المحفوظة لسورة (" + item.getSurahName() + ")؟");
        builder.setPositiveButton("حذف", (dialog, which) -> {
            DownloadManager.getInstance().deleteDownloadedSurah(this, item.getSurahNumber(), item.getReciterId(), item.getEnglishName());
            Toast.makeText(this, "تم حذف الملفات المحفوظة", Toast.LENGTH_SHORT).show();
            loadDownloadsData();
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    @Override
    public void onDownloadStateChanged(int surahNumber, String reciterId, DownloadManager.State state) {
        loadDownloadsData();
    }

    @Override
    public void onDownloadProgress(int surahNumber, String reciterId, int downloadedAyahs, int totalAyahs, int progressPercentage) {
        loadDownloadsData();
    }

    @Override
    public void onDownloadCompleted(int surahNumber, String reciterId) {
        loadDownloadsData();
    }

    @Override
    public void onDownloadFailed(int surahNumber, String reciterId, String errorMessage) {
        loadDownloadsData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloadsData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadManager.getInstance().removeListener(this);
    }
}
