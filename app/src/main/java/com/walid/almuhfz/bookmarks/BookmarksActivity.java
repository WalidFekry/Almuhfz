package com.walid.almuhfz.bookmarks;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.db.entity.BookmarkEntity;
import com.walid.almuhfz.learning.LearningActivity;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.learning.models.Reciter;
import com.walid.almuhfz.learning.models.SoraDetails;
import com.walid.almuhfz.utils.AppExecutors;
import com.walid.almuhfz.utils.PreferencesManager;
import com.walid.almuhfz.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements BookmarksAdapter.ActionListener {

    private TextView chipAll, chipHifz, chipMurajaa, chipItqan;
    private RecyclerView bookmarksRecyclerView;
    private ProgressBar bookmarksProgressBar;
    private LinearLayout emptyStateContainer;

    private BookmarksAdapter adapter;
    private String currentCategoryFilter = "الكل";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        initViews();
        setupListeners();
        setupRecyclerView();

        loadBookmarks();
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        chipAll = findViewById(R.id.chipAll);
        chipHifz = findViewById(R.id.chipHifz);
        chipMurajaa = findViewById(R.id.chipMurajaa);
        chipItqan = findViewById(R.id.chipItqan);

        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView);
        bookmarksProgressBar = findViewById(R.id.bookmarksProgressBar);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
    }

    private void setupListeners() {
        chipAll.setOnClickListener(v -> filterCategory("الكل"));
        chipHifz.setOnClickListener(v -> filterCategory("حفظ"));
        chipMurajaa.setOnClickListener(v -> filterCategory("مراجعة"));
        chipItqan.setOnClickListener(v -> filterCategory("إتقان"));
    }

    private void setupRecyclerView() {
        adapter = new BookmarksAdapter(this);
        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookmarksRecyclerView.setAdapter(adapter);
    }

    private void filterCategory(String category) {
        this.currentCategoryFilter = category;
        updateChipStyles();
        loadBookmarks();
    }

    private void updateChipStyles() {
        int primaryBg = ContextCompat.getColor(this, R.color.primary_color);
        int surfaceBg = ContextCompat.getColor(this, R.color.surface_card);
        int textOnPrimary = ContextCompat.getColor(this, R.color.text_on_primary);
        int textPrimary = ContextCompat.getColor(this, R.color.text_primary_color);

        chipAll.setBackgroundTintList(ColorStateList.valueOf(currentCategoryFilter.equals("الكل") ? primaryBg : surfaceBg));
        chipAll.setTextColor(currentCategoryFilter.equals("الكل") ? textOnPrimary : textPrimary);

        chipHifz.setBackgroundTintList(ColorStateList.valueOf(currentCategoryFilter.equals("حفظ") ? primaryBg : surfaceBg));
        chipHifz.setTextColor(currentCategoryFilter.equals("حفظ") ? textOnPrimary : textPrimary);

        chipMurajaa.setBackgroundTintList(ColorStateList.valueOf(currentCategoryFilter.equals("مراجعة") ? primaryBg : surfaceBg));
        chipMurajaa.setTextColor(currentCategoryFilter.equals("مراجعة") ? textOnPrimary : textPrimary);

        chipItqan.setBackgroundTintList(ColorStateList.valueOf(currentCategoryFilter.equals("إتقان") ? primaryBg : surfaceBg));
        chipItqan.setTextColor(currentCategoryFilter.equals("إتقان") ? textOnPrimary : textPrimary);
    }

    private void loadBookmarks() {
        bookmarksProgressBar.setVisibility(View.VISIBLE);

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<BookmarkEntity> list;
            if (currentCategoryFilter.equals("الكل")) {
                list = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAllBookmarks();
            } else {
                list = QuranDatabase.getInstance(getApplicationContext()).quranDao().getBookmarksByCategory(currentCategoryFilter);
            }

            final List<BookmarkEntity> resultList = (list != null) ? list : new ArrayList<>();

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                bookmarksProgressBar.setVisibility(View.GONE);
                adapter.setItems(resultList);

                if (resultList.isEmpty()) {
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    bookmarksRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateContainer.setVisibility(View.GONE);
                    bookmarksRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    public void onPlayAyahClicked(BookmarkEntity bookmark) {
        if (bookmark == null) return;

        String reciterId = (bookmark.getReciterId() != null && !bookmark.getReciterId().isEmpty())
                ? bookmark.getReciterId()
                : PreferencesManager.getSavedReciterId(this);

        String reciterName = PreferencesManager.getSavedReciterName(this);

        Reciter reciter = new Reciter();
        reciter.setIdentifier(reciterId);
        reciter.setName(reciterName);

        SoraDetails soraDetails = new SoraDetails();
        soraDetails.setNumber(bookmark.getSurahNumber());
        soraDetails.setName(bookmark.getSurahName());

        LearningData data = new LearningData(
                reciter,
                soraDetails,
                bookmark.getSurahNumber(),
                1,
                286,
                1,
                1
        );
        data.setInitialPlayIndex(bookmark.getNumberInSurah() - 1);

        Intent intent = new Intent(this, LearningActivity.class);
        intent.putExtra("learning_data", data);
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(BookmarkEntity bookmark) {
        if (bookmark == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("حذف العلامة 🗑️");
        builder.setMessage("هل أنت متأكد من حذف هذه العلامة المرجعية؟");
        builder.setPositiveButton("حذف", (dialog, which) -> {
            AppExecutors.getInstance().diskIO().execute(() -> {
                QuranDatabase.getInstance(getApplicationContext()).quranDao().deleteBookmark(bookmark.getId());
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(this, "تم حذف العلامة المرجعية", Toast.LENGTH_SHORT).show();
                    loadBookmarks();
                });
            });
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
}
