package com.walid.almuhfz.learning;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.walid.almuhfz.db.mapper.AyahMapper;
import com.walid.almuhfz.learning.models.Ayah;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.utils.AppExecutors;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LearningActivity extends AppCompatActivity implements AyasAdapter.Interaction, JcPlayerManagerListener {

    private static final String TAG = "LearningActivity";

    private AyasAdapter ayasAdapter;
    private LearningData learningData;
    private TextView reciterName;
    private RecyclerView ayasRecyclerView;
    private JcPlayerView player;
    private int currentPlayIndex = 0;
    private int currentRepeatAyaIndex = 1;
    private int currentRepeatSoraIndex = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable audioCompletedRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);

        if (!initIntentData()) {
            Toast.makeText(this, "تعذر تحميل بيانات التعلم", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<AyahEntity> roomData = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAyahsBySurah(surahNumber);
            List<Ayah> selectedAyasList = new ArrayList<>();

            int from = learningData.getSelectedFrom();
            int to = learningData.getSelectedTo();

            if (roomData != null && !roomData.isEmpty()) {
                for (AyahEntity entity : roomData) {
                    if (entity.getNumberInSurah() >= from && entity.getNumberInSurah() <= to) {
                        selectedAyasList.add(AyahMapper.fromEntity(entity));
                    }
                }
            } else if (learningData.getSoraDetails().getAyahs() != null) {
                List<Ayah> allAyahs = learningData.getSoraDetails().getAyahs();
                for (int i = 0; i < allAyahs.size(); i++) {
                    if (i >= (from - 1) && i <= (to - 1)) {
                        selectedAyasList.add(allAyahs.get(i));
                    }
                }
            }

            Log.d(TAG, "selectedAyasList size: " + selectedAyasList.size());
            learningData.getSoraDetails().setAyahs(selectedAyasList);

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;
                setAdapter();
                playAudio();
            });
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
    }

    private void setAdapter() {
        if (learningData.getSoraDetails().getAyahs() == null) {
            learningData.getSoraDetails().setAyahs(new ArrayList<>());
        }
        ayasAdapter = new AyasAdapter(learningData.getSoraDetails().getAyahs(), this);
        ayasRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ayasRecyclerView.setAdapter(ayasAdapter);

        if (learningData.getSelectedReciter() != null && learningData.getSelectedReciter().getName() != null) {
            reciterName.setText(learningData.getSelectedReciter().getName());
        }
    }

    private void initViews() {
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        reciterName = findViewById(R.id.reciterName);
        ayasRecyclerView = findViewById(R.id.ayasRecyclerView);
        player = findViewById(R.id.jcplayer);

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
        String audioUrl = ayah.getAudio();

        String reciterId = (learningData.getSelectedReciter() != null && learningData.getSelectedReciter().getIdentifier() != null)
                ? learningData.getSelectedReciter().getIdentifier() : "";
        String englishName = (learningData.getSoraDetails().getEnglishName() != null)
                ? learningData.getSoraDetails().getEnglishName() : "surah";

        File localFile = new File(getApplicationContext().getExternalFilesDir(null),
                "QuranAudio/" + reciterId + "/" + englishName + "_ayah_" + ayah.getNumberInSurah() + ".mp3");

        String title = (ayah.getText() != null) ? ayah.getText() : "آية " + (index + 1);

        if (localFile.exists()) {
            return JcAudio.createFromFilePath(title, localFile.getAbsolutePath());
        } else if (audioUrl != null && !audioUrl.isEmpty()) {
            return JcAudio.createFromURL(title, audioUrl);
        }
        return null;
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

        audioCompletedRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;

            if (currentRepeatAyaIndex < learningData.getRepeatAya()) {
                currentRepeatAyaIndex += 1;
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

        handler.postDelayed(audioCompletedRunnable, 200);
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
    public void onPlaying(@NonNull JcStatus jcStatus) {}

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
