package com.walid.almuhfz.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.BookmarkEntity;
import com.walid.almuhfz.learning.LearningActivity;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.learning.models.Reciter;
import com.walid.almuhfz.learning.models.SearchMatch;
import com.walid.almuhfz.learning.models.SearchResponse;
import com.walid.almuhfz.learning.models.SoraDetails;
import com.walid.almuhfz.learning.networking.RetrofitClient;
import com.walid.almuhfz.utils.AppExecutors;
import com.walid.almuhfz.utils.PreferencesManager;
import com.walid.almuhfz.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements SearchAdapter.ActionListener {

    private static final String TAG = "SearchActivity";

    private EditText etSearchQuery;
    private ImageButton btnSearch;
    private RecyclerView searchResultsRecyclerView;
    private ProgressBar searchProgressBar;
    private LinearLayout emptyStateContainer;
    private TextView tvEmptyTitle, tvEmptyDesc;

    private SearchAdapter adapter;
    private Call<SearchResponse> activeSearchCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerView();

        setupListeners();
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnSearch = findViewById(R.id.btnSearch);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchProgressBar = findViewById(R.id.searchProgressBar);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);
    }

    private void setupRecyclerView() {
        adapter = new SearchAdapter(this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> performSearch());

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    performSearch();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public static String normalizeArabicText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E8\u06EA-\u06ED]", "")
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ٱ', 'ا')
                .replace('ة', 'ه')
                .replace('ى', 'ي')
                .trim();
    }

    private void performSearch() {
        String rawQuery = etSearchQuery.getText().toString().trim();
        if (rawQuery.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            emptyStateContainer.setVisibility(View.VISIBLE);
            tvEmptyTitle.setText("ابحث في آيات القرآن الكريم");
            tvEmptyDesc.setText("ادخل الكلمة أو العبارة المطلوبة للوصول المباشر للآية والسورة.");
            return;
        }

        searchProgressBar.setVisibility(View.VISIBLE);
        String normalizedQuery = normalizeArabicText(rawQuery);

        if (activeSearchCall != null) {
            activeSearchCall.cancel();
        }

        activeSearchCall = RetrofitClient.getInstance().getApi().searchQuran(rawQuery);
        activeSearchCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<SearchMatch> matches = response.body().getData().getMatches();
                    if (matches != null && !matches.isEmpty()) {
                        List<AyahEntity> entities = new ArrayList<>();
                        for (SearchMatch match : matches) {
                            if (match != null) {
                                int surahNum = (match.getSurah() != null && match.getSurah().getNumber() != null) ? match.getSurah().getNumber() : 1;
                                AyahEntity entity = new AyahEntity(
                                        match.getNumber() != null ? match.getNumber() : 0,
                                        match.getNumberInSurah() != null ? match.getNumberInSurah() : 0,
                                        match.getText() != null ? match.getText() : "",
                                        "",
                                        surahNum
                                );
                                entities.add(entity);
                            }
                        }
                        searchProgressBar.setVisibility(View.GONE);
                        displaySearchResults(entities);
                        return;
                    }
                }

                performLocalSearch(rawQuery, normalizedQuery);
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                performLocalSearch(rawQuery, normalizedQuery);
            }
        });
    }

    private void performLocalSearch(String rawQuery, String normalizedQuery) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<AyahEntity> allAyahs = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAllAyahsForSearch();
            List<AyahEntity> results = new ArrayList<>();

            if (allAyahs != null) {
                for (AyahEntity ayah : allAyahs) {
                    if (ayah != null && ayah.getText() != null) {
                        String normalizedAyahText = normalizeArabicText(ayah.getText());
                        if (normalizedAyahText.contains(normalizedQuery) || ayah.getText().contains(rawQuery)) {
                            results.add(ayah);
                            if (results.size() >= 100) break;
                        }
                    }
                }
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                searchProgressBar.setVisibility(View.GONE);
                displaySearchResults(results);
            });
        });
    }

    private void displaySearchResults(List<AyahEntity> results) {
        List<AyahEntity> finalResults = (results != null) ? results : new ArrayList<>();
        adapter.setItems(finalResults);

        if (finalResults.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            tvEmptyTitle.setText("لا توجد نتائج بحث مطابقة");
            tvEmptyDesc.setText("تأكد من كتابة الكلمة بشكل صحيح، أو جرب البحث بكلمة أخرى.");
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOpenAyahClicked(AyahEntity ayah, String surahName, int surahNumber) {
        if (ayah == null) return;

        String savedReciterId = PreferencesManager.getSavedReciterId(this);
        String savedReciterName = PreferencesManager.getSavedReciterName(this);

        Reciter reciter = new Reciter();
        reciter.setIdentifier(savedReciterId);
        reciter.setName(savedReciterName);

        SoraDetails soraDetails = new SoraDetails();
        soraDetails.setNumber(surahNumber);
        soraDetails.setName(surahName);

        LearningData data = new LearningData(
                reciter,
                soraDetails,
                surahNumber,
                1,
                286,
                1,
                1
        );
        data.setInitialPlayIndex(ayah.getNumberInSurah() - 1);

        Intent intent = new Intent(this, LearningActivity.class);
        intent.putExtra("learning_data", data);
        startActivity(intent);
    }

    @Override
    public void onBookmarkClicked(AyahEntity ayah, String surahName, int surahNumber) {
        if (ayah == null) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            BookmarkEntity bookmark = new BookmarkEntity(
                    surahNumber,
                    surahName,
                    ayah.getNumberInSurah(),
                    ayah.getText(),
                    "ar.alafasy",
                    "علامة من البحث",
                    "مراجعة",
                    System.currentTimeMillis()
            );
            QuranDatabase.getInstance(getApplicationContext()).quranDao().insertBookmark(bookmark);

            AppExecutors.getInstance().mainThread().execute(() -> {
                Toast.makeText(this, "تمت إضافة الآية " + ayah.getNumberInSurah() + " إلى المحفوظات 🔖", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeSearchCall != null) {
            activeSearchCall.cancel();
        }
    }
}
