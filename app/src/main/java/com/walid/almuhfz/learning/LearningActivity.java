package com.walid.almuhfz.learning;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jean.jcplayer.JcPlayerManagerListener;
import com.example.jean.jcplayer.general.JcStatus;
import com.example.jean.jcplayer.model.JcAudio;
import com.example.jean.jcplayer.view.JcPlayerView;
import com.walid.almuhfz.R;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.BookmarkEntity;
import com.walid.almuhfz.db.mapper.AyahMapper;
import com.walid.almuhfz.download.DownloadManager;
import com.walid.almuhfz.learning.models.Ayah;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.learning.models.SoraDetailsResponse;
import com.walid.almuhfz.learning.networking.RetrofitClient;
import com.walid.almuhfz.utils.AppExecutors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LearningActivity extends AppCompatActivity implements AyasAdapter.Interaction, JcPlayerManagerListener {

    private static final String TAG = "LearningActivity";
    private static final String PREF_FONT_SIZE = "pref_quran_font_size";
    private static final String PREF_FONT_FAMILY = "pref_quran_font_family";
    private static final String PREF_REPEAT_INTERVAL = "pref_quran_repeat_interval";

    private AyasAdapter ayasAdapter;
    private LearningData learningData;
    private TextView reciterName;
    private RecyclerView ayasRecyclerView;
    private JcPlayerView player;
    private Button btnBookmarkCurrent, btnRepeatInterval, btnFontCustomization;

    private int currentPlayIndex = 0;
    private int currentRepeatAyaIndex = 1;
    private int currentRepeatSoraIndex = 1;

    private int pauseIntervalSeconds = 0;
    private float currentFontSizeSp = 22f;
    private int currentFontResId = R.font.uthmanii;

    private long playStartTimestamp = 0L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable audioCompletedRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);

        if (!initIntentData()) {
            Toast.makeText(this, "تعذر تحميل بيانات التعلم", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        currentFontSizeSp = prefs.getFloat(PREF_FONT_SIZE, 22f);
        currentFontResId = prefs.getInt(PREF_FONT_FAMILY, R.font.uthmanii);
        pauseIntervalSeconds = prefs.getInt(PREF_REPEAT_INTERVAL, 0);

        initViews();
        loadAyahsData();
    }

    private boolean initIntentData() {
        Intent intent = getIntent();
        if (intent == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            learningData = intent.getSerializableExtra("learning_data", LearningData.class);
        } else {
            Serializable s = intent.getSerializableExtra("learning_data");
            if (s instanceof LearningData) {
                learningData = (LearningData) s;
            }
        }

        return learningData != null && learningData.getSoraDetails() != null && learningData.getSelectedReciter() != null;
    }

    private void loadAyahsData() {
        int surahNumber = learningData.getSoraDetails().getNumber() != null ? learningData.getSoraDetails().getNumber() : 1;
        String reciterId = (learningData.getSelectedReciter() != null && learningData.getSelectedReciter().getIdentifier() != null)
                ? learningData.getSelectedReciter().getIdentifier() : "ar.alafasy";

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<AyahEntity> roomData = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAyahsBySurahAndReciter(surahNumber, reciterId);

            List<Ayah> selectedAyasList = new ArrayList<>();

            int from = learningData.getSelectedFrom();
            int to = learningData.getSelectedTo();

            if (roomData != null && !roomData.isEmpty()) {
                for (AyahEntity entity : roomData) {
                    if (entity.getNumberInSurah() >= from && entity.getNumberInSurah() <= to) {
                        selectedAyasList.add(AyahMapper.fromEntity(entity));
                    }
                }
            } else if (learningData.getSoraDetails().getAyahs() != null && !learningData.getSoraDetails().getAyahs().isEmpty()) {
                List<Ayah> allAyahs = learningData.getSoraDetails().getAyahs();
                for (int i = 0; i < allAyahs.size(); i++) {
                    Ayah a = allAyahs.get(i);
                    if (a != null && a.getNumberInSurah() >= from && a.getNumberInSurah() <= to) {
                        selectedAyasList.add(a);
                    }
                }
            }

            if (!selectedAyasList.isEmpty()) {
                learningData.getSoraDetails().setAyahs(selectedAyasList);
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentPlayIndex = Math.min(learningData.getInitialPlayIndex(), selectedAyasList.size() - 1);
                    if (currentPlayIndex < 0) currentPlayIndex = 0;
                    setAdapter();
                    playAudio();
                });
            } else {
                fetchSurahFromApi(surahNumber, reciterId, from, to);
            }
        });
    }

    private void fetchSurahFromApi(int surahNumber, String reciterId, int from, int to) {
        String activeReciter = (reciterId != null && !reciterId.isEmpty()) ? reciterId : "ar.alafasy";

        RetrofitClient.getInstance().getApi().getSoraDetails(surahNumber, activeReciter).enqueue(new Callback<SoraDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<SoraDetailsResponse> call, @NonNull Response<SoraDetailsResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Ayah> allAyahs = response.body().getData().getAyahs();
                    List<Ayah> selectedAyasList = new ArrayList<>();
                    if (allAyahs != null) {
                        for (Ayah a : allAyahs) {
                            if (a != null && a.getNumberInSurah() >= from && a.getNumberInSurah() <= to) {
                                selectedAyasList.add(a);
                            }
                        }
                    }

                    if (!selectedAyasList.isEmpty()) {
                        learningData.getSoraDetails().setAyahs(selectedAyasList);
                        currentPlayIndex = Math.min(learningData.getInitialPlayIndex(), selectedAyasList.size() - 1);
                        if (currentPlayIndex < 0) currentPlayIndex = 0;
                        setAdapter();
                        playAudio();
                        return;
                    }
                }

                showMessage("تعذر تحميل الآيات، يرجى التأكد من الاتصال بالإنترنت.");
            }

            @Override
            public void onFailure(@NonNull Call<SoraDetailsResponse> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                showMessage("تعذر الاتصال بالشبكة لتحميل السورة.");
            }
        });
    }

    private void playAudio() {
        if (isFinishing() || isDestroyed()) return;

        ArrayList<JcAudio> jcAudios = new ArrayList<>();
        JcAudio firstAudio = getNextAudio(0);
        if (firstAudio != null) {
            jcAudios.add(firstAudio);
        }
        if (!jcAudios.isEmpty()) {
            try {
                player.initPlaylist(jcAudios, this);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing player playlist", e);
            }
        } else {
            showMessage("القائمة فارغة، لن يتم تشغيل المشغل.");
        }
        JcAudio audioToPlay = getNextAudio(currentPlayIndex);
        if (audioToPlay != null) {
            try {
                player.playAudio(audioToPlay);
            } catch (Exception e) {
                Log.e(TAG, "Error playing audio", e);
            }
        }

        if (ayasAdapter != null) {
            ayasAdapter.setSelectedAyaIndex(currentPlayIndex);
        }
        if (ayasRecyclerView != null) {
            ayasRecyclerView.post(() -> ayasRecyclerView.scrollToPosition(currentPlayIndex));
        }
    }

    private void setAdapter() {
        if (learningData.getSoraDetails().getAyahs() == null) {
            learningData.getSoraDetails().setAyahs(new ArrayList<>());
        }
        ayasAdapter = new AyasAdapter(learningData.getSoraDetails().getAyahs(), this);
        ayasAdapter.setFontSizeSp(currentFontSizeSp);
        ayasAdapter.setFontResId(currentFontResId);
        ayasRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ayasRecyclerView.setAdapter(ayasAdapter);

        String sName = (learningData.getSoraDetails() != null && learningData.getSoraDetails().getName() != null && !learningData.getSoraDetails().getName().trim().isEmpty())
                ? learningData.getSoraDetails().getName()
                : "سورة " + learningData.getSelectedSoraNumber();

        String rName = (learningData.getSelectedReciter() != null && learningData.getSelectedReciter().getName() != null && !learningData.getSelectedReciter().getName().trim().isEmpty())
                ? learningData.getSelectedReciter().getName()
                : "القارئ";

        reciterName.setText(rName + " - " + sName);
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        reciterName = findViewById(R.id.reciterName);
        ayasRecyclerView = findViewById(R.id.ayasRecyclerView);
        player = findViewById(R.id.jcplayer);

        btnBookmarkCurrent = findViewById(R.id.btnBookmarkCurrent);
        btnRepeatInterval = findViewById(R.id.btnRepeatInterval);
        btnFontCustomization = findViewById(R.id.btnFontCustomization);

        btnBookmarkCurrent.setOnClickListener(v -> showAddBookmarkDialog());
        btnRepeatInterval.setOnClickListener(v -> showRepeatIntervalDialog());
        btnFontCustomization.setOnClickListener(v -> showFontSizeDialog());

        player.findViewById(R.id.btnNext).setOnClickListener(view -> {
            List<Ayah> ayahs = learningData.getSoraDetails().getAyahs();
            if (ayahs == null || ayahs.isEmpty()) return;

            if (currentPlayIndex >= ayahs.size() - 1) {
                currentPlayIndex = 0;
            } else {
                currentPlayIndex += 1;
            }
            currentRepeatAyaIndex = 1;

            JcAudio audioToPlay = getNextAudio(currentPlayIndex);
            if (audioToPlay != null) {
                player.playAudio(audioToPlay);
            }
            ayasRecyclerView.smoothScrollToPosition(currentPlayIndex);
            if (ayasAdapter != null) {
                ayasAdapter.setSelectedAyaIndex(currentPlayIndex);
            }
        });

        player.findViewById(R.id.btnPrev).setOnClickListener(view -> {
            List<Ayah> ayahs = learningData.getSoraDetails().getAyahs();
            if (ayahs == null || ayahs.isEmpty()) return;

            if (currentPlayIndex == 0) {
                currentRepeatAyaIndex = 1;
            } else {
                currentPlayIndex -= 1;
                currentRepeatAyaIndex = 1;
            }

            JcAudio audioToPlay = getNextAudio(currentPlayIndex);
            if (audioToPlay != null) {
                player.playAudio(audioToPlay);
            }
            ayasRecyclerView.smoothScrollToPosition(currentPlayIndex);
            if (ayasAdapter != null) {
                ayasAdapter.setSelectedAyaIndex(currentPlayIndex);
            }
        });
    }

    private void showAddBookmarkDialog() {
        if (learningData == null || learningData.getSoraDetails() == null) return;
        List<Ayah> ayahs = learningData.getSoraDetails().getAyahs();
        if (ayahs == null || currentPlayIndex >= ayahs.size()) return;

        Ayah currentAyah = ayahs.get(currentPlayIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("إضافة إلى المحفوظات 🔖");

        final EditText etNote = new EditText(this);
        etNote.setHint("إضافة ملاحظة شخصية (اختياري)");

        builder.setView(etNote);

        CharSequence[] categories = new CharSequence[]{"حفظ", "مراجعة", "إتقان"};
        final String[] selectedCategory = {"حفظ"};

        builder.setSingleChoiceItems(categories, 0, (dialog, which) -> {
            if (which >= 0 && which < categories.length) {
                selectedCategory[0] = categories[which].toString();
            }
        });

        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String note = etNote.getText().toString().trim();
            int surahNum = learningData.getSoraDetails().getNumber();
            String surahName = learningData.getSoraDetails().getName();
            int numberInSurah = currentAyah.getNumberInSurah();
            String ayahText = currentAyah.getText();
            String reciterId = (learningData.getSelectedReciter() != null) ? learningData.getSelectedReciter().getIdentifier() : "";

            AppExecutors.getInstance().diskIO().execute(() -> {
                BookmarkEntity entity = new BookmarkEntity(
                        surahNum,
                        surahName,
                        numberInSurah,
                        ayahText,
                        reciterId,
                        note,
                        selectedCategory[0],
                        System.currentTimeMillis()
                );
                QuranDatabase.getInstance(getApplicationContext()).quranDao().insertBookmark(entity);

                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(this, "تمت إضافة الآية " + numberInSurah + " إلى المحفوظات", Toast.LENGTH_SHORT).show();
                });
            });
        });

        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void showRepeatIntervalDialog() {
        CharSequence[] options = new CharSequence[]{"بدون فاصل زمني", "2 ثانية", "4 ثواني", "6 ثواني", "8 ثواني", "10 ثواني"};
        int selected = (pauseIntervalSeconds == 2) ? 1 :
                (pauseIntervalSeconds == 4) ? 2 :
                (pauseIntervalSeconds == 6) ? 3 :
                (pauseIntervalSeconds == 8) ? 4 :
                (pauseIntervalSeconds == 10) ? 5 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر الفاصل الزمني بين التكرارات ⏱️");
        builder.setSingleChoiceItems(options, selected, (dialog, which) -> {
            pauseIntervalSeconds = (which == 1) ? 2 :
                    (which == 2) ? 4 :
                    (which == 3) ? 6 :
                    (which == 4) ? 8 :
                    (which == 5) ? 10 : 0;

            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putInt(PREF_REPEAT_INTERVAL, pauseIntervalSeconds).apply();

            if (pauseIntervalSeconds == 0) {
                Toast.makeText(this, "تم إيقاف الفاصل الزمني بين التكرارات", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "تم ضبط وحفظ الفاصل الزمني إلى " + pauseIntervalSeconds + " ثواني", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void showFontSizeDialog() {
        CharSequence[] options = new CharSequence[]{"نوع خط القراءة", "حجم الخط القرآني"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تنسيق الخط والقراءة 📖");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showFontTypefaceDialog();
            } else {
                showFontSizeSelectionDialog();
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void showFontTypefaceDialog() {
        CharSequence[] fonts = new CharSequence[]{"الخط العثماني الاصلي (Uthmanii)", "الخط القرآني المبسّط (Quran)"};
        int selected = (currentFontResId == R.font.quran) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر خط القراءة المصحفي 🔤");
        builder.setSingleChoiceItems(fonts, selected, (dialog, which) -> {
            currentFontResId = (which == 1) ? R.font.quran : R.font.uthmanii;
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putInt(PREF_FONT_FAMILY, currentFontResId).apply();

            if (ayasAdapter != null) {
                ayasAdapter.setFontResId(currentFontResId);
            }
            dialog.dismiss();
            Toast.makeText(this, "تم تغيير خط القراءة بنجاح", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void showFontSizeSelectionDialog() {
        CharSequence[] sizes = new CharSequence[]{"صغير", "متوسط", "كبير", "كبير جدا"};
        int selected = (currentFontSizeSp <= 18f) ? 0 : (currentFontSizeSp <= 22f) ? 1 : (currentFontSizeSp <= 26f) ? 2 : 3;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر حجم الخط القرآني 📏");
        builder.setSingleChoiceItems(sizes, selected, (dialog, which) -> {
            currentFontSizeSp = (which == 0) ? 18f : (which == 1) ? 22f : (which == 2) ? 26f : 30f;
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putFloat(PREF_FONT_SIZE, currentFontSizeSp).apply();

            if (ayasAdapter != null) {
                ayasAdapter.setFontSizeSp(currentFontSizeSp);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private JcAudio getNextAudio(int index) {
        if (learningData == null || learningData.getSoraDetails() == null) return null;

        List<Ayah> ayahs = learningData.getSoraDetails().getAyahs();
        if (ayahs == null || ayahs.isEmpty()) {
            showMessage("خطأ: قائمة الآيات فارغة");
            return null;
        }

        if (index < 0 || index >= ayahs.size()) {
            showMessage("خطأ: رقم الآية غير صالح");
            return null;
        }

        Ayah ayah = ayahs.get(index);

        String reciterId = (learningData.getSelectedReciter() != null && learningData.getSelectedReciter().getIdentifier() != null)
                ? learningData.getSelectedReciter().getIdentifier() : "ar.alafasy";
        String englishName = (learningData.getSoraDetails().getEnglishName() != null)
                ? learningData.getSoraDetails().getEnglishName() : "surah";

        File localFile = new File(getApplicationContext().getExternalFilesDir(null),
                "QuranAudio/" + reciterId + "/" + englishName + "_ayah_" + ayah.getNumberInSurah() + ".mp3");

        String title = (ayah.getText() != null) ? ayah.getText() : "آية " + (index + 1);

        if (DownloadManager.isAudioFileValid(localFile)) {
            return JcAudio.createFromFilePath(title, localFile.getAbsolutePath());
        }

        String audioUrl = ayah.getAudio();
        if (audioUrl == null || audioUrl.trim().isEmpty() || !audioUrl.contains(reciterId)) {
            if (ayah.getNumber() != null && ayah.getNumber() > 0) {
                audioUrl = "https://cdn.islamic.network/quran/audio/128/" + reciterId + "/" + ayah.getNumber() + ".mp3";
            }
        }

        if (audioUrl != null && !audioUrl.isEmpty()) {
            return JcAudio.createFromURL(title, audioUrl);
        } else {
            showMessage("الملف الصوتي غير متاح حالياً. يرجى التأكد من الاتصال بالإنترنت.");
            return null;
        }
    }

    @Override
    public void onAyaClicked(Ayah ayah, int position) {
        currentRepeatAyaIndex = 1;
        currentPlayIndex = position;
        JcAudio audioToPlay = getNextAudio(position);
        if (audioToPlay != null) {
            player.playAudio(audioToPlay);
        }
    }

    @Override
    public void onCompletedAudio() {
        if (audioCompletedRunnable != null) {
            handler.removeCallbacks(audioCompletedRunnable);
        }

        long extraDelayMs = Math.max(0, pauseIntervalSeconds * 1000L);

        audioCompletedRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;

            int repeatLimit = learningData.getRepeatAya();
            boolean isInfiniteRepeat = (repeatLimit == -1 || repeatLimit == 0);

            if (isInfiniteRepeat || currentRepeatAyaIndex < repeatLimit) {
                if (!isInfiniteRepeat) {
                    currentRepeatAyaIndex += 1;
                }
            } else {
                currentPlayIndex += 1;
                currentRepeatAyaIndex = 1;
            }

            List<Ayah> ayahs = learningData.getSoraDetails().getAyahs();
            if (ayahs != null && currentPlayIndex < ayahs.size()) {
                JcAudio audioToPlay = getNextAudio(currentPlayIndex);
                if (audioToPlay != null) {
                    player.playAudio(audioToPlay);
                }
                ayasRecyclerView.smoothScrollToPosition(currentPlayIndex);
                if (ayasAdapter != null) {
                    ayasAdapter.setSelectedAyaIndex(currentPlayIndex);
                }
            } else {
                currentPlayIndex = 0;
                if (ayasAdapter != null) {
                    ayasAdapter.setSelectedAyaIndex(0);
                }

                if (currentRepeatSoraIndex < learningData.getRepeatSora()) {
                    currentRepeatSoraIndex += 1;
                    JcAudio audioToPlay = getNextAudio(currentPlayIndex);
                    if (audioToPlay != null) {
                        player.playAudio(audioToPlay);
                    }
                    ayasRecyclerView.smoothScrollToPosition(currentPlayIndex);
                } else {
                    finish();
                }
            }
        };

        handler.postDelayed(audioCompletedRunnable, 200 + extraDelayMs);
    }

    private void showMessage(String message) {
        if (!isFinishing() && !isDestroyed() && message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onContinueAudio(@NonNull JcStatus jcStatus) {}

    @Override
    public void onJcpError(@NonNull Throwable throwable) {
        Log.e(TAG, "JcPlayer error: " + throwable.getMessage());
    }

    @Override
    public void onPaused(@NonNull JcStatus jcStatus) {}

    @Override
    public void onPlaying(@NonNull JcStatus jcStatus) {
        playStartTimestamp = System.currentTimeMillis();
    }

    @Override
    public void onPreparedAudio(@NonNull JcStatus jcStatus) {}

    @Override
    public void onStopped(@NonNull JcStatus jcStatus) {}

    @Override
    public void onTimeChanged(@NonNull JcStatus jcStatus) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioCompletedRunnable != null) {
            handler.removeCallbacks(audioCompletedRunnable);
        }
        if (player != null) {
            try {
                player.kill();
            } catch (Exception e) {
                Log.e(TAG, "Error killing player", e);
            }
        }
    }
}
