package com.walid.almuhfz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.ReciterEntity;
import com.walid.almuhfz.db.entity.SurahEntity;
import com.walid.almuhfz.db.mapper.AyahMapper;
import com.walid.almuhfz.db.mapper.ReciterMapper;
import com.walid.almuhfz.learning.LearningActivity;
import com.walid.almuhfz.learning.models.Ayah;
import com.walid.almuhfz.learning.models.LearningData;
import com.walid.almuhfz.learning.models.Reciter;
import com.walid.almuhfz.learning.models.ReciterResponse;
import com.walid.almuhfz.learning.models.Sora;
import com.walid.almuhfz.learning.models.SoraDetails;
import com.walid.almuhfz.learning.models.SoraDetailsResponse;
import com.walid.almuhfz.learning.networking.QuranUtils;
import com.walid.almuhfz.learning.networking.RetrofitClient;
import com.walid.almuhfz.notifications_messages.NotificationsMessagesActivity;
import com.walid.almuhfz.utils.AppExecutors;
import com.walid.almuhfz.utils.ThemeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import guy4444.smartrate.SmartRate;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import softpro.naseemali.ShapedNavigationView;
import softpro.naseemali.ShapedViewSettings;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;
    private static final int MAX_PROMPT_COUNT = 10;

    private final List<Sora> soraList = QuranUtils.getSoraList();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ImageView rateApp;
    private List<Reciter> reciterList;
    private String englishName;
    private SoraDetails soraDetails;
    private TextView reciterTextView, soraTextView, startLearn, fromAyaTextView, repeatSoraTextView, repeatAyaTextView, toAyaTextView, startLearnDone;
    private int lastReciter = 0;
    private Reciter selectedReciter;
    private int selectedSoraNumber = 1;
    private int selectedFrom = 1;
    private int selectedTo = 1;
    private int repeatAya = 1;
    private int repeatSora = 1;

    private AppCompatImageButton downloadButton;
    private ProgressBar downloadProgressBar;
    private TextView downloadStatusText;

    private Dialog loadingDialog;
    private Runnable ratingRunnable;

    private Call<ReciterResponse> recitersCall;
    private Call<SoraDetailsResponse> soraDetailsCall;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.show_notifications) {
            Intent i = new Intent(this, NotificationsMessagesActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainbar);

        initViews();
        setListeners();
        setupToolbar();
        setupDrawer();
        setupNavigationView();

        getListOfReciters();
        promptUserForRating();
        requestNotificationPermission();

        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseMessaging subscribe error", e);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "رائع! 🎉 الآن ستتلقى اقتباسات ورسائل تفاؤل تحفزك يوميًا! 😊", Toast.LENGTH_LONG).show();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    showSettingsDialog();
                } else {
                    Toast.makeText(this, "😔 بدون الإشعارات، قد تفوتك رسائل تحفيزية واقتباسات تمنحك الطاقة! يمكنك تفعيلها لاحقًا من الإعدادات. ⚡", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تفعيل الإشعارات 📢");
        builder.setMessage("لكي تصلك اقتباسات يومية ورسائل تحفيزية، تحتاج إلى تفعيل الإشعارات من الإعدادات.");

        builder.setPositiveButton("فتح الإعدادات", (dialog, which) -> {
            dialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        builder.setNegativeButton("لاحقًا", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void promptUserForRating() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long lastPromptTime = prefs.getLong("last_rating_prompt", 0);
        int promptCount = prefs.getInt("rating_prompt_count", 0);
        long currentTime = System.currentTimeMillis();

        if (promptCount < MAX_PROMPT_COUNT && (currentTime - lastPromptTime >= ONE_DAY_MILLIS)) {
            ratingRunnable = () -> {
                if (!isFinishing() && !isDestroyed()) {
                    SmartRate.Rate(MainActivity.this, "قيم تجربتك معنا!", "نحن نسعى لجعل تطبيق المحفظ أفضل كل يوم، ويساعدنا تقييمك في تقديم تجربة مميزة لك!", "قيم الآن", "دعمك لنا يحفزنا! اترك لنا تقييماً رائعاً على جوجل بلاي", "اضغط هنا للتقييم", "ليس الآن", "شكراً لدعمك!", Color.parseColor("#305A23"), 2);
                    saveLastPromptData(promptCount + 1);
                }
            };
            handler.postDelayed(ratingRunnable, 5000);
        }
    }

    private void saveLastPromptData(int newCount) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putLong("last_rating_prompt", System.currentTimeMillis())
                .putInt("rating_prompt_count", newCount)
                .apply();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupDrawer() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigationView() {
        ShapedNavigationView shapedNavigationView = findViewById(R.id.nav_view);
        shapedNavigationView.getSettings().setShapeType(ShapedViewSettings.ROUNDED_RECT);
        shapedNavigationView.setNavigationItemSelectedListener(this);
    }

    public File createSurahFolder(Context context) {
        if (selectedReciter == null || selectedReciter.getIdentifier() == null) {
            return null;
        }
        File folder = new File(context.getExternalFilesDir(null), "QuranAudio/" + selectedReciter.getIdentifier());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public void downloadAyahAudio(String url, File outputFile, Runnable onComplete) {
        Call<ResponseBody> call = RetrofitClient.getInstance().getApi().downloadFileWithDynamicUrl(url);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        boolean writtenToDisk = writeResponseBodyToDisk(response.body(), outputFile);
                        if (writtenToDisk) {
                            Log.d(TAG, "File downloaded: " + outputFile.getAbsolutePath());
                            if (onComplete != null) {
                                AppExecutors.getInstance().mainThread().execute(onComplete);
                            }
                        } else {
                            Log.e(TAG, "Failed to write the file to disk");
                        }
                    });
                } else {
                    if (response.errorBody() != null) {
                        response.errorBody().close();
                    }
                    Log.e(TAG, "Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Download failed: " + t.getMessage());
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, File outputFile) {
        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] fileReader = new byte[4096];
            int read;

            while ((read = inputStream.read(fileReader)) != -1) {
                outputStream.write(fileReader, 0, read);
            }

            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "IOException writing file: " + e.getMessage());
            return false;
        }
    }

    public void downloadSurahAudio(Context context, List<Ayah> ayahs) {
        if (ayahs == null || ayahs.isEmpty() || soraDetails == null || selectedReciter == null) {
            Toast.makeText(context, "لا توجد تفاصيل متاحة للتحميل", Toast.LENGTH_SHORT).show();
            return;
        }

        File folder = createSurahFolder(context);
        if (folder == null) {
            Toast.makeText(context, "تعذر إنشاء مجلد الحفظ", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalAyahs = ayahs.size();
        AtomicInteger downloadedCount = new AtomicInteger(0);

        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadStatusText.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(0);
        downloadStatusText.setText(getString(R.string.download_progress_format, 0, totalAyahs));

        for (Ayah ayah : ayahs) {
            String url = ayah.getAudio();
            String fileName = (soraDetails.getEnglishName() != null ? soraDetails.getEnglishName() : "surah")
                    + "_ayah_" + ayah.getNumberInSurah() + ".mp3";
            File file = new File(folder, fileName);

            Runnable updateProgressRunnable = () -> {
                int current = downloadedCount.incrementAndGet();
                int progress = (int) ((current / (float) totalAyahs) * 100);

                if (!isFinishing() && !isDestroyed()) {
                    downloadProgressBar.setProgress(progress);
                    downloadStatusText.setText(getString(R.string.download_progress_format, current, totalAyahs));
                    if (current == totalAyahs) {
                        Toast.makeText(context, "تم تحميل السورة بالكامل", Toast.LENGTH_LONG).show();
                        downloadProgressBar.setVisibility(View.GONE);
                        downloadStatusText.setVisibility(View.GONE);
                    }
                }
            };

            if (!file.exists()) {
                downloadAyahAudio(url, file, updateProgressRunnable);
            } else {
                updateProgressRunnable.run();
            }
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                ReciterEntity reciter = new ReciterEntity(selectedReciter.getIdentifier(), selectedReciter.getName());
                QuranDatabase.getInstance(getApplicationContext()).quranDao().insertReciter(reciter);

                SurahEntity surah = new SurahEntity(soraDetails.getNumber(), soraDetails.getName(), reciter.getId(), soraDetails.getEnglishName());
                long surahId = QuranDatabase.getInstance(getApplicationContext()).quranDao().insertSurah(surah);

                if (surahId == -1) {
                    return;
                }

                List<AyahEntity> entities = new ArrayList<>();
                for (Ayah ayah : soraDetails.getAyahs()) {
                    AyahEntity entity = new AyahEntity(
                            ayah.getNumber() != null ? ayah.getNumber() : 0,
                            ayah.getNumberInSurah() != null ? ayah.getNumberInSurah() : 0,
                            ayah.getText() != null ? ayah.getText() : "",
                            ayah.getAudio() != null ? ayah.getAudio() : "",
                            (int) surahId
                    );
                    entities.add(entity);
                }

                QuranDatabase.getInstance(getApplicationContext()).quranDao().insertAyahs(entities);
            } catch (Exception e) {
                Log.e(TAG, "Error saving downloaded surah to Room", e);
            }
        });
    }

    private void setListeners() {
        downloadButton.setOnClickListener(view -> {
            if (soraDetails != null && soraDetails.getAyahs() != null) {
                downloadSurahAudio(this, soraDetails.getAyahs());
            } else {
                Toast.makeText(this, "برجاء اختيار السورة أولاً", Toast.LENGTH_SHORT).show();
            }
        });

        reciterTextView.setOnClickListener(view -> selectReciter());
        soraTextView.setOnClickListener(view -> selectSora());
        fromAyaTextView.setOnClickListener(view -> selectFromAya());
        toAyaTextView.setOnClickListener(view -> selectToAya());
        repeatAyaTextView.setOnClickListener(view -> selectAyaRepeat());
        repeatSoraTextView.setOnClickListener(view -> selectSoraRepeat());

        startLearn.setOnClickListener(view -> {
            if (selectedReciter == null || soraDetails == null) {
                Toast.makeText(this, "يرجى اختيار القارئ والسورة أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LearningActivity.class);
            LearningData data = new LearningData(selectedReciter, soraDetails, selectedSoraNumber, selectedFrom, selectedTo, repeatAya, repeatSora);
            intent.putExtra("learning_data", data);
            startActivity(intent);
        });

        rateApp.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.walid.almuhfz"))));
    }

    private void initViews() {
        reciterTextView = findViewById(R.id.reciter_text_view);
        soraTextView = findViewById(R.id.sora_text_view);
        fromAyaTextView = findViewById(R.id.fromAyaTextView);
        toAyaTextView = findViewById(R.id.toAyaTextView);
        repeatAyaTextView = findViewById(R.id.repeatAyaTextView);
        repeatSoraTextView = findViewById(R.id.repeatSoraTextView);
        startLearn = findViewById(R.id.startLearn);
        rateApp = findViewById(R.id.rating);
        startLearnDone = findViewById(R.id.startLearnDone);
        downloadButton = findViewById(R.id.download_button);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        downloadStatusText = findViewById(R.id.downloadStatusText);
    }

    private void getListOfReciters() {
        showLoading();
        setViewsState(false, 0.5f);
        toggleLearnButtons(true);

        if (recitersCall != null) {
            recitersCall.cancel();
        }

        recitersCall = RetrofitClient.getInstance().getApi().getListOfReciter();
        recitersCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ReciterResponse> call, @NonNull Response<ReciterResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    logErrorResponse(response);
                    handleError("حدث خطأ أثناء تحميل البيانات. الرجاء المحاولة مرة أخرى.");
                    return;
                }

                List<Reciter> allReciters = response.body().getData();
                if (allReciters != null) {
                    reciterList = filterRecitersByFormat(allReciters, "audio");

                    if (!reciterList.isEmpty()) {
                        updateUIWithReciter(reciterList.get(0));
                        return;
                    }
                }

                handleError("حدث خطأ أثناء تحميل البيانات. الرجاء المحاولة مرة أخرى.");
            }

            @Override
            public void onFailure(@NonNull Call<ReciterResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                getLocalReciters();
                Log.e(TAG, "API call failed: " + t.getLocalizedMessage());
            }
        });
    }

    private void getLocalReciters() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ReciterEntity> downloadedEntities = QuranDatabase.getInstance(getApplicationContext()).quranDao().getDownloadedReciters();

            List<Reciter> downloadedReciters = new ArrayList<>();
            for (ReciterEntity entity : downloadedEntities) {
                downloadedReciters.add(ReciterMapper.fromEntity(entity));
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (downloadedReciters.isEmpty()) {
                    handleError("يبدو أن هناك مشكلة في الاتصال بالإنترنت. يرجى التحقق من اتصالك وحاول مرة أخرى.");
                } else {
                    reciterList = downloadedReciters;
                    updateUIWithReciter(reciterList.get(0));
                }
            });
        });
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

    private void setViewsState(boolean enable, float alpha) {
        soraTextView.setEnabled(enable);
        fromAyaTextView.setEnabled(enable);
        toAyaTextView.setEnabled(enable);
        soraTextView.setAlpha(alpha);
        fromAyaTextView.setAlpha(alpha);
        toAyaTextView.setAlpha(alpha);
    }

    private void toggleLearnButtons(boolean isLoading) {
        if (isLoading) {
            startLearn.setVisibility(View.GONE);
            startLearnDone.setVisibility(View.VISIBLE);
        } else {
            startLearn.setVisibility(View.VISIBLE);
            startLearnDone.setVisibility(View.GONE);
        }
    }

    private List<Reciter> filterRecitersByFormat(List<Reciter> reciters, String format) {
        List<Reciter> filteredList = new ArrayList<>();
        if (reciters == null) return filteredList;

        for (Reciter reciter : reciters) {
            if (reciter != null && format.equalsIgnoreCase(reciter.getFormat())) {
                filteredList.add(reciter);
            }
        }
        return filteredList;
    }

    private void logErrorResponse(Response<?> response) {
        if (response != null) {
            Log.e(TAG, "Response Error: " + response.message());
            Log.e(TAG, "Response Code: " + response.code());
        }
    }

    private void updateUIWithReciter(Reciter reciter) {
        if (reciter == null) return;
        reciterTextView.setText(reciter.getName());
        selectedReciter = reciter;
        getSoraDetails();
    }

    private void getSoraDetails() {
        if (selectedReciter == null || selectedReciter.getIdentifier() == null) {
            handleError("خطأ في القارئ المحدد");
            return;
        }

        showLoading();
        toggleLearnButtons(true);

        if (soraDetailsCall != null) {
            soraDetailsCall.cancel();
        }

        soraDetailsCall = RetrofitClient.getInstance().getApi().getSoraDetails(selectedSoraNumber, selectedReciter.getIdentifier());
        soraDetailsCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SoraDetailsResponse> call, @NonNull Response<SoraDetailsResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    SoraDetails data = response.body().getData();
                    soraDetails = data;

                    soraTextView.setText(data.getName());
                    int ayahCount = (data.getAyahs() != null) ? data.getAyahs().size() : 0;
                    toAyaTextView.setText(String.valueOf(ayahCount));
                    selectedTo = ayahCount;
                    selectedFrom = 1;
                    fromAyaTextView.setText("1");

                    hideLoading();
                    setViewsState(true, 1f);
                    toggleLearnButtons(false);
                } else {
                    handleError("حدث خطأ أثناء تحميل البيانات. الرجاء المحاولة مرة أخرى.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<SoraDetailsResponse> call, @NonNull Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;

                getLocalSoraDetails();
                Log.e(TAG, "API call failed: " + t.getLocalizedMessage());
            }
        });
    }

    private void getLocalSoraDetails() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (selectedReciter == null || selectedReciter.getIdentifier() == null) return;

            List<SurahEntity> downloadedSurahs = QuranDatabase.getInstance(getApplicationContext()).quranDao().getDownloadedSurahsByReciter(selectedReciter.getIdentifier());

            List<Sora> localSoraList = new ArrayList<>();
            for (SurahEntity surahEntity : downloadedSurahs) {
                localSoraList.add(new Sora(surahEntity.getSurahNumber(), surahEntity.getSurahName(), surahEntity.getEnglishName()));
            }

            List<AyahEntity> roomData = QuranDatabase.getInstance(getApplicationContext()).quranDao().getAyahsBySurah(selectedSoraNumber);
            List<Ayah> selectedAyasList = new ArrayList<>();
            for (AyahEntity entity : roomData) {
                selectedAyasList.add(AyahMapper.fromEntity(entity));
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (downloadedSurahs.isEmpty()) {
                    handleError("يبدو أن هناك مشكلة في الاتصال بالإنترنت. يرجى التحقق من اتصالك وحاول مرة أخرى.");
                } else {
                    soraList.clear();
                    soraList.addAll(localSoraList);

                    int ayahCount = selectedAyasList.size();
                    toAyaTextView.setText(String.valueOf(ayahCount));
                    selectedTo = ayahCount;
                    selectedFrom = 1;
                    fromAyaTextView.setText("1");
                    soraDetails = new SoraDetails();
                    soraDetails.setNumber(selectedSoraNumber);
                    soraDetails.setAyahs(selectedAyasList);
                    if (englishName == null && !soraList.isEmpty()) {
                        soraDetails.setEnglishName(soraList.get(0).getEnglishName());
                    } else {
                        soraDetails.setEnglishName(englishName);
                    }
                    hideLoading();
                    setViewsState(true, 1f);
                    toggleLearnButtons(false);
                }
            });
        });
    }

    private void handleError(String message) {
        if (!isFinishing() && !isDestroyed() && message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        hideLoading();
    }

    private void selectReciter() {
        if (reciterList == null || reciterList.isEmpty()) {
            getListOfReciters();
            return;
        }
        CharSequence[] items = new CharSequence[reciterList.size()];
        for (int i = 0; i < reciterList.size(); i++) {
            items[i] = reciterList.get(i).getName();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, lastReciter, (d, n) -> {
            if (n >= 0 && n < reciterList.size()) {
                selectedReciter = reciterList.get(n);
                reciterTextView.setText(selectedReciter.getName());
                lastReciter = n;
                getSoraDetails();
            }
            d.dismiss();
        });
        adb.setNegativeButton("إلغاء", null);
        adb.setTitle("اختر القارئ");
        adb.show();
    }

    private void selectSora() {
        if (soraList.isEmpty()) {
            Toast.makeText(this, "قائمة السور غير متاحة", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[soraList.size()];
        for (int i = 0; i < soraList.size(); i++) {
            items[i] = soraList.get(i).getName();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, selectedSoraNumber - 1, (d, n) -> {
            if (n >= 0 && n < soraList.size()) {
                String name = soraList.get(n).getName();
                soraTextView.setText(name);
                selectedSoraNumber = n + 1;
                englishName = soraList.get(n).getEnglishName();
                getSoraDetails();
            }
            d.dismiss();
        });
        adb.setNegativeButton("إلغاء", null);
        adb.setTitle("اختر سورة");
        adb.show();
    }

    private void selectFromAya() {
        if (soraDetails == null || soraDetails.getAyahs() == null || soraDetails.getAyahs().isEmpty()) {
            Toast.makeText(this, "الرجاء اختيار السورة أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Ayah> ayahs = soraDetails.getAyahs();
        CharSequence[] items = new CharSequence[ayahs.size()];
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("ar-EG"));

        for (int i = 0; i < ayahs.size(); i++) {
            items[i] = nf.format(i + 1) + " - " + ayahs.get(i).getText();
        }

        int selectedIndex = Math.max(0, Math.min(selectedFrom - 1, ayahs.size() - 1));

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, selectedIndex, (d, n) -> {
            int newFrom = n + 1;
            if (selectedTo >= newFrom) {
                selectedFrom = newFrom;
                fromAyaTextView.setText(String.valueOf(selectedFrom));
            } else {
                Toast.makeText(this, "يجب أن تكون الآية النهائية بعد الآية الابتدائية", Toast.LENGTH_LONG).show();
            }
            d.dismiss();
        });
        adb.setNegativeButton("إلغاء", null);
        adb.setTitle("من الآية");
        adb.show();
    }

    private void selectToAya() {
        if (soraDetails == null || soraDetails.getAyahs() == null || soraDetails.getAyahs().isEmpty()) {
            Toast.makeText(this, "الرجاء اختيار السورة أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Ayah> ayahs = soraDetails.getAyahs();
        CharSequence[] items = new CharSequence[ayahs.size()];
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("ar-EG"));

        for (int i = 0; i < ayahs.size(); i++) {
            items[i] = nf.format(i + 1) + " - " + ayahs.get(i).getText();
        }

        int selectedIndex = Math.max(0, Math.min(selectedTo - 1, ayahs.size() - 1));

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, selectedIndex, (d, n) -> {
            int newTo = n + 1;
            if (newTo >= selectedFrom) {
                selectedTo = newTo;
                toAyaTextView.setText(String.valueOf(selectedTo));
            } else {
                Toast.makeText(this, "يجب أن تكون الآية النهائية بعد الآية الابتدائية", Toast.LENGTH_LONG).show();
            }
            d.dismiss();
        });
        adb.setNegativeButton("إلغاء", null);
        adb.setTitle("إلى الآية");
        adb.show();
    }

    @SuppressLint("SetTextI18n")
    private void selectAyaRepeat() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        edittext.setGravity(Gravity.CENTER);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        edittext.setHint("ادخل رقم التكرار");

        alert.setTitle("عدد تكرار الآية الواحدة");
        alert.setView(edittext);

        alert.setPositiveButton("تم", (dialog, whichButton) -> {
            String textValue = edittext.getText().toString().trim();
            if (textValue.isEmpty()) {
                Toast.makeText(this, "برجاء أدخال رقم صالح", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int value = Integer.parseInt(textValue);
                if (value <= 0 || value > 100) {
                    Toast.makeText(this, "برجاء إدخال عدد تكرار بين 1 و 100", Toast.LENGTH_SHORT).show();
                } else {
                    repeatAya = value;
                    repeatAyaTextView.setText("عدد التكرار لكل آية = " + repeatAya);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "برجاء أدخال رقم صالح", Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNegativeButton("إلغاء", (dialog, whichButton) -> dialog.dismiss());
        alert.show();
    }

    @SuppressLint("SetTextI18n")
    private void selectSoraRepeat() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        edittext.setGravity(Gravity.CENTER);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        edittext.setHint("ادخل رقم التكرار");

        alert.setTitle("عدد تكرار السورة");
        alert.setView(edittext);

        alert.setPositiveButton("تم", (dialog, whichButton) -> {
            String textValue = edittext.getText().toString().trim();
            if (textValue.isEmpty()) {
                Toast.makeText(this, "برجاء أدخال رقم صالح", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int value = Integer.parseInt(textValue);
                if (value <= 0 || value > 100) {
                    Toast.makeText(this, "برجاء إدخال عدد تكرار بين 1 و 100", Toast.LENGTH_SHORT).show();
                } else {
                    repeatSora = value;
                    repeatSoraTextView.setText("عدد التكرار = " + repeatSora);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "برجاء أدخال رقم صالح", Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNegativeButton("إلغاء", (dialog, whichButton) -> dialog.dismiss());
        alert.show();
    }

    private void showThemeSelectionDialog() {
        CharSequence[] options = new CharSequence[]{"فاتح (Light)", "داكن (Dark)", "تلقائي حسب النظام (System)"};
        int currentMode = ThemeManager.getSavedThemeMode(this);
        int selectedIndex = (currentMode == ThemeManager.THEME_LIGHT) ? 0 :
                (currentMode == ThemeManager.THEME_DARK) ? 1 : 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر مظهر التطبيق");
        builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
            int newMode = (which == 0) ? ThemeManager.THEME_LIGHT :
                    (which == 1) ? ThemeManager.THEME_DARK : ThemeManager.THEME_SYSTEM;
            ThemeManager.saveAndApplyTheme(MainActivity.this, newMode);
            dialog.dismiss();
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.theme_switch) {
            showThemeSelectionDialog();
        } else if (id == R.id.maktbtiplus) {
            Intent o = new Intent(Intent.ACTION_VIEW);
            o.setData(Uri.parse("https://walid-fekry.com/maktbti-plus/"));
            startActivity(o);
        } else if (id == R.id.facegroub) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/groups/440403217380641")));
        } else if (id == R.id.teleee) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/App_Maktbti")));
        } else if (id == R.id.ayak) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.co/2xVh8rqVLS")));
        } else if (id == R.id.ratee) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.walid.almuhfz")));
        } else if (id == R.id.shareee) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "المٌحفظ للقرآن - المصحف المعلم 💙");
            sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.aboutapp) + "\n\nحمل التطبيق من خلال هذا الرابط أو من متجر جوجل بلاي 👇  https://play.google.com/store/apps/details?id=com.walid.almuhfz\n");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (id == R.id.mass) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "prowalidfekry@gmail.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "رسالة إلى مبرمج تطبيق المٌحفظ 📲");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "");
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        } else if (id == R.id.sysa) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/almuhfz/home")));
        } else if (id == R.id.walidmore) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.co/US34fsUZeW")));
        } else if (id == R.id.notification_messages) {
            Intent i = new Intent(this, NotificationsMessagesActivity.class);
            startActivity(i);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ratingRunnable != null) {
            handler.removeCallbacks(ratingRunnable);
        }
        if (recitersCall != null) {
            recitersCall.cancel();
        }
        if (soraDetailsCall != null) {
            soraDetailsCall.cancel();
        }
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception ignored) {
            }
            loadingDialog = null;
        }
    }
}
