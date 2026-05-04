package com.walid.almuhfz;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private static final int MAX_PROMPT_COUNT = 10; // Maximum number of times to show
    private final List<Sora> soraList = QuranUtils.getSoraList();
    private final Handler handler = new Handler(Looper.getMainLooper());
    ImageView rateApp;
    List<Reciter> reciterList;
    String englishName;
    SoraDetails soraDetails;
    TextView reciterTextView, soraTextView, startLearn, fromAyaTextView, repeatSoraTextView, repeatAyaTextView, toAyaTextView, startLearnDone;
    int lastReciter = 0;
    Reciter selectedReciter;
    int selectedSoraNumber = 1;
    int selectedFrom = 1;
    int selectedTo = 1;
    int repeatAya = 1;
    int repeatSora = 1;
    AppCompatImageButton downloadButton;
    ProgressBar downloadProgressBar;
    TextView downloadStatusText;
    private Dialog loadingDialog;
    private Runnable ratingRunnable;

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
        FirebaseMessaging.getInstance().subscribeToTopic("all");
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {// Android 13 (API 33) or higher
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request permission
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
                // فحص إذا كان المستخدم اختار "Don't ask again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    showSettingsDialog(); // عرض رسالة تنقل المستخدم للإعدادات
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

        // Ensure the prompt count does not exceed the limit and at least 24 hours have passed
        if (promptCount < MAX_PROMPT_COUNT && (currentTime - lastPromptTime >= ONE_DAY_MILLIS)) {
            ratingRunnable = () -> {
                if (!isFinishing()) {
                    SmartRate.Rate(MainActivity.this, "قيم تجربتك معنا!", "نحن نسعى لجعل تطبيق المحفظ أفضل كل يوم، ويساعدنا تقييمك في تقديم تجربة مميزة لك!", "قيم الآن", "دعمك لنا يحفزنا! اترك لنا تقييماً رائعاً على جوجل بلاي", "اضغط هنا للتقييم", "ليس الآن", "شكراً لدعمك!", Color.parseColor("#305A23"), 2);

                    // Update rating prompt data (last prompt time + prompt count)
                    saveLastPromptData(promptCount + 1);
                }
            };

            handler.postDelayed(ratingRunnable, 5000); // Execute after 5 seconds
        }
    }

    // Save the time of the last request and the number of times it was shown
    private void saveLastPromptData(int newCount) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_rating_prompt", System.currentTimeMillis());
        editor.putInt("rating_prompt_count", newCount);
        editor.apply();
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
        File folder = new File(context.getExternalFilesDir(null), "QuranAudio/" + selectedReciter.getIdentifier());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public void downloadAyahAudio(String url, File outputFile, Runnable onComplete) {
        Call<ResponseBody> call = RetrofitClient.getInstance().getApi().downloadFileWithDynamicUrl(url);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(), outputFile);
                    if (writtenToDisk) {
                        Log.d("Download", "File downloaded: " + outputFile.getAbsolutePath());
                        if (onComplete != null) onComplete.run();
                    } else {
                        Log.e("Download", "Failed to write the file to disk");
                    }
                } else {
                    Log.e("Download", "Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Download", "Download failed: " + t.getMessage());
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, File outputFile) {
        try (InputStream inputStream = body.byteStream(); OutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] fileReader = new byte[4096];
            int read;

            while ((read = inputStream.read(fileReader)) != -1) {
                outputStream.write(fileReader, 0, read);
            }

            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e("Download", "IOException writing file: " + e.getMessage());
            return false;
        }
    }

    public void downloadSurahAudio(Context context, List<Ayah> ayahs) {
        File folder = createSurahFolder(context);

        int totalAyahs = ayahs.size();
        AtomicInteger downloadedCount = new AtomicInteger(0);

        // Show progress
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadStatusText.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(0);
        downloadStatusText.setText("0 / " + totalAyahs);

        for (Ayah ayah : ayahs) {
            String url = ayah.getAudio();
            String fileName = soraDetails.getEnglishName() + "_ayah_" + ayah.getNumberInSurah() + ".mp3";
            File file = new File(folder, fileName);

            if (!file.exists()) {
                downloadAyahAudio(url, file, () -> {
                    int current = downloadedCount.incrementAndGet();
                    int progress = (int) ((current / (float) totalAyahs) * 100);

                    runOnUiThread(() -> {
                        downloadProgressBar.setProgress(progress);
                        downloadStatusText.setText(current + " / " + totalAyahs);
                        if (current == totalAyahs) {
                            Toast.makeText(context, "تم تحميل السورة بالكامل", Toast.LENGTH_LONG).show();
                            downloadProgressBar.setVisibility(View.GONE);
                            downloadStatusText.setVisibility(View.GONE);
                        }
                    });
                });
            } else {
                int current = downloadedCount.incrementAndGet();
                int progress = (int) ((current / (float) totalAyahs) * 100);
                runOnUiThread(() -> {
                    downloadProgressBar.setProgress(progress);
                    downloadStatusText.setText(current + " / " + totalAyahs);
                    if (current == totalAyahs) {
                        Toast.makeText(context, "تم تحميل السورة بالكامل", Toast.LENGTH_LONG).show();
                        downloadProgressBar.setVisibility(View.GONE);
                        downloadStatusText.setVisibility(View.GONE);
                    }
                });
            }
        }

        // Save to Room in background thread
        new Thread(() -> {
            // 1. Save reciter
            ReciterEntity reciter = new ReciterEntity(selectedReciter.getIdentifier(), selectedReciter.getName());
            QuranDatabase.getInstance(this).quranDao().insertReciter(reciter);

            // 2. Save surah
            SurahEntity surah = new SurahEntity(soraDetails.getNumber(), soraDetails.getName(), reciter.getId(), soraDetails.getEnglishName());
            long surahId = QuranDatabase.getInstance(this).quranDao().insertSurah(surah);

            if (surahId == -1) {
                // السورة موجودة قبل كده
                return;
            }

            // 3. Save ayahs
            List<AyahEntity> entities = new ArrayList<>();
            for (Ayah ayah : soraDetails.getAyahs()) {
                AyahEntity entity = new AyahEntity(ayah.getNumber(), ayah.getNumberInSurah(), ayah.getText(), ayah.getAudio(), (int) surahId);
                entities.add(entity);
            }

            QuranDatabase.getInstance(this).quranDao().insertAyahs(entities);
        }).start();

    }

    private void setListeners() {
        downloadButton.setOnClickListener(view -> {
            downloadSurahAudio(this, soraDetails.getAyahs());
        });

        reciterTextView.setOnClickListener(view -> {
            selectReciter();
        });

        soraTextView.setOnClickListener(view -> {
            selectSora();
        });

        fromAyaTextView.setOnClickListener(view -> {
            selectFromAya();
        });

        toAyaTextView.setOnClickListener(view -> {
            selectToAya();
        });

        repeatAyaTextView.setOnClickListener(view -> {
            selectAyaRepeat();
        });

        repeatSoraTextView.setOnClickListener(view -> {
            selectSoraRepeat();
        });

        startLearn.setOnClickListener(view -> {
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
        RetrofitClient.getInstance().getApi().getListOfReciter().enqueue(new Callback<ReciterResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReciterResponse> call, @NonNull Response<ReciterResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    logErrorResponse(response);
                    handleError("حدث خطأ أثناء تحميل البيانات. الرجاء المحاولة مرة أخرى.");
                    return;
                }

                List<Reciter> allReciters = response.body().getData();
                reciterList = filterRecitersByFormat(allReciters, "audio");

                Log.d(TAG, "Total reciters: " + allReciters.size());
                Log.d(TAG, "Filtered reciters (audio): " + reciterList.size());

                if (!reciterList.isEmpty()) {
                    updateUIWithReciter(reciterList.get(0));
                } else {
                    Log.d(TAG, "No reciters found with format = audio");
                    handleError("حدث خطأ أثناء تحميل البيانات. الرجاء المحاولة مرة أخرى.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReciterResponse> call, @NonNull Throwable t) {
                getLocalReciters();
                Log.e(TAG, "API call failed: " + t.getLocalizedMessage());
            }
        });
    }

    private void getLocalReciters() {
        new Thread(() -> {
            List<ReciterEntity> downloadedEntities = QuranDatabase.getInstance(getApplicationContext()).quranDao().getDownloadedReciters();

            List<Reciter> downloadedReciters = new ArrayList<>();
            for (ReciterEntity entity : downloadedEntities) {
                downloadedReciters.add(ReciterMapper.fromEntity(entity));
            }
            runOnUiThread(() -> {
                if (downloadedReciters.isEmpty()) {
                    handleError("يبدو أن هناك مشكلة في الاتصال بالإنترنت. يرجى التحقق من اتصالك وحاول مرة أخرى.");
                } else {
                    reciterList = downloadedReciters;
                    updateUIWithReciter(reciterList.get(0));
                }
            });
        }).start();

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
        if (loadingDialog != null && loadingDialog.isShowing()) {
            runOnUiThread(() -> { // يضمن التنفيذ على UI Thread
                try {
                    Context context = loadingDialog.getContext();
                    if (context instanceof Activity activity) {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            loadingDialog.dismiss();
                        }
                    } else {
                        loadingDialog.dismiss();
                    }
                    loadingDialog = null;
                } catch (Exception e) {
                    Log.e(TAG, "خطأ أثناء إغلاق الـ Dialog", e);
                }
            });
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

    /**
     * Filters the reciters list based on the given format.
     */
    private List<Reciter> filterRecitersByFormat(List<Reciter> reciters, String format) {
        List<Reciter> filteredList = new ArrayList<>();
        for (Reciter reciter : reciters) {
            if (format.equalsIgnoreCase(reciter.getFormat())) {
                filteredList.add(reciter);
            }
        }
        return filteredList;
    }

    /**
     * Logs API error response details.
     */
    private void logErrorResponse(Response<?> response) {
        Log.e(TAG, "Response Error: " + response.message());
        Log.e(TAG, "Response Code: " + response.code());
    }

    /**
     * Updates the UI with the selected reciter.
     */
    private void updateUIWithReciter(Reciter reciter) {
        reciterTextView.setText(reciter.getName());
        selectedReciter = reciter;
        getSoraDetails();
    }


    private void getSoraDetails() {
        RetrofitClient.getInstance().getApi().getSoraDetails(selectedSoraNumber, selectedReciter.getIdentifier()).enqueue(new Callback<SoraDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<SoraDetailsResponse> call, @NonNull Response<SoraDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    SoraDetails data = response.body().getData();
                    soraDetails = data;

                    soraTextView.setText(data.getName());
                    int ayahCount = data.getAyahs().size();
                    toAyaTextView.setText(String.valueOf(ayahCount));
                    selectedTo = ayahCount;
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
                getLocalSoraDetails();
                Log.e(TAG, "API call failed: " + t.getLocalizedMessage());
            }
        });
    }

    private void getLocalSoraDetails() {
        new Thread(() -> {
            List<SurahEntity> downloadedSurahs = QuranDatabase.getInstance(this).quranDao().getDownloadedSurahsByReciter(selectedReciter.getIdentifier());

            soraList.clear();
            for (SurahEntity surahEntity : downloadedSurahs) {
                soraList.add(new Sora(surahEntity.getSurahNumber(), surahEntity.getSurahName(), surahEntity.getEnglishName()));
            }

            List<AyahEntity> roomData = QuranDatabase.getInstance(this).quranDao().getAyahsBySurah(selectedSoraNumber);
            List<Ayah> selectedAyasList = new ArrayList<>();
            for (AyahEntity entity : roomData) {
                selectedAyasList.add(AyahMapper.fromEntity(entity));
            }

            runOnUiThread(() -> {
                if (downloadedSurahs.isEmpty()) {
                    handleError("يبدو أن هناك مشكلة في الاتصال بالإنترنت. يرجى التحقق من اتصالك وحاول مرة أخرى.");
                } else {
                    int ayahCount = selectedAyasList.size();
                    toAyaTextView.setText(String.valueOf(ayahCount));
                    selectedTo = ayahCount;
                    fromAyaTextView.setText("1");
                    soraDetails = new SoraDetails();
                    soraDetails.setNumber(selectedSoraNumber);
                    soraDetails.setAyahs(selectedAyasList);
                    if (englishName == null) {
                        soraDetails.setEnglishName(soraList.get(0).getEnglishName());
                    } else {
                        soraDetails.setEnglishName(englishName);
                    }
                    hideLoading();
                    setViewsState(true, 1f);
                    toggleLearnButtons(false);
                }
            });
        }).start();
    }

    private void handleError(String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        hideLoading();
    }


    private void selectReciter() {
        if (reciterList == null) {
            getListOfReciters();
            return;
        }
        CharSequence[] items = new CharSequence[reciterList.size()];
        for (int i = 0; i <= reciterList.size() - 1; i++) {
            items[i] = reciterList.get(i).getName();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, lastReciter, (d, n) -> {
            selectedReciter = reciterList.get(n);
            reciterTextView.setText(reciterList.get(n).getName());
            lastReciter = n;
            getSoraDetails();
            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("اختر القارئ");
        adb.show();
    }


    private void selectSora() {
        CharSequence[] items = new CharSequence[soraList.size()];
        for (int i = 0; i <= soraList.size() - 1; i++) {
            items[i] = soraList.get(i).getName();
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, 0, (d, n) -> {
            String name = soraList.get(n).getName();
            soraTextView.setText(name);
            selectedSoraNumber = n + 1;
            englishName = soraList.get(n).getEnglishName();
            showLoading();
            toggleLearnButtons(true);
            getSoraDetails();
            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("اختر سورة");
        adb.show();
    }

    private void selectFromAya() {
        CharSequence[] items = new CharSequence[0];
        NumberFormat nf = NumberFormat.getInstance(new Locale("ar", "EG"));
        try {
            items = new CharSequence[soraDetails.getAyahs().size()];
            for (int i = 0; i <= soraDetails.getAyahs().size() - 1; i++) {

                items[i] = nf.format(i + 1) + " = " + soraDetails.getAyahs().get(i).getText();
            }
        } catch (Exception e) {
            Log.d(TAG, "selectFromAya: " + e.getMessage());
        }
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setSingleChoiceItems(items, selectedFrom - 1, (d, n) -> {
            selectedFrom = n + 1;

            if (selectedTo >= selectedFrom) {
                fromAyaTextView.setText(String.valueOf(n + 1));
            } else {
                Toast.makeText(this, "يجب ان يكون الايه النهائيه بعد الايه الابتدائيه", Toast.LENGTH_LONG).show();
            }

            d.dismiss();
        });
        adb.setNegativeButton("الغاء", null);
        adb.setTitle("من الايه");
        adb.show();

    }

    private void selectToAya() {
        try {
            NumberFormat nf = NumberFormat.getInstance(new Locale("ar", "EG"));
            CharSequence[] items = new CharSequence[soraDetails.getAyahs().size()];
            for (int i = 0; i <= soraDetails.getAyahs().size() - 1; i++) {

                items[i] = nf.format(i + 1) + " = " + soraDetails.getAyahs().get(i).getText();
            }
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setSingleChoiceItems(items, selectedTo - 1, (d, n) -> {
                selectedTo = n + 1;


                if (selectedTo >= selectedFrom) {
                    toAyaTextView.setText(String.valueOf(n + 1));
                } else {
                    Toast.makeText(this, "يجب ان يكون الايه النهائيه بعد الايه الابتدائيه", Toast.LENGTH_LONG).show();
                }
                d.dismiss();
            });
            adb.setNegativeButton("الغاء", null);
            adb.setTitle("الى الايه");
            adb.show();

        } catch (Exception e) {
            Log.d(TAG, "selectToAya: " + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void selectAyaRepeat() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        edittext.setGravity(Gravity.CENTER);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);

        edittext.setHint("ادخل رقم التكرار");
        alert.setTitle("عدد تكرار الآيه الواحده");

        alert.setView(edittext);

        alert.setPositiveButton("تم", (dialog, whichButton) -> {
            String textValue = edittext.getText().toString();
            if (textValue.isEmpty() || textValue.equals("0") || Integer.parseInt(textValue) > 604) {
                Toast.makeText(this, "برجاء اختيار رقم صفحة صالح", Toast.LENGTH_SHORT).show();
            } else {
                repeatAya = Integer.parseInt(textValue);
                repeatAyaTextView.setText("عدد التكرار لكل آيه" + " = " + textValue);
            }
        });

        alert.setNegativeButton("الغاء", (dialog, whichButton) -> {
            dialog.dismiss();
            // what ever you want to do with No option.
        });

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
            String textValue = edittext.getText().toString();
            if (textValue.isEmpty() || textValue.equals("0") || Integer.parseInt(textValue) > 604) {
                Toast.makeText(this, "برجاء اختيار رقم صفحة صالح", Toast.LENGTH_SHORT).show();
            } else {
                repeatSora = Integer.parseInt(textValue);
                repeatSoraTextView.setText("عدد التكرار " + " = " + textValue);
            }
        });

        alert.setNegativeButton("الغاء", (dialog, whichButton) -> {
            dialog.dismiss();
            // what ever you want to do with No option.
        });

        alert.show();
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.maktbtiplus) {
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
            sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.aboutapp) + "\n" + "\n" + "حمل التطبيق من خلال هذا الرابط أو من متجر جوجل بلاي \uD83D\uDC47  https://play.google.com/store/apps/details?id=com.walid.almuhfz \n");
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
    }
}