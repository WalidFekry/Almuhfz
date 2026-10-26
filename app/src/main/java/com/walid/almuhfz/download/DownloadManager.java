package com.walid.almuhfz.download;

import android.content.Context;
import android.util.Log;

import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.ReciterEntity;
import com.walid.almuhfz.db.entity.SurahEntity;
import com.walid.almuhfz.learning.models.Ayah;
import com.walid.almuhfz.learning.models.Reciter;
import com.walid.almuhfz.learning.models.SoraDetails;
import com.walid.almuhfz.learning.networking.RetrofitClient;
import com.walid.almuhfz.utils.AppExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadManager {

    private static final String TAG = "DownloadManager";
    private static final long MIN_VALID_AUDIO_BYTES = 2048L; // 2 KB minimum for valid audio segment

    public enum State {
        IDLE,
        DOWNLOADING,
        COMPLETED,
        PAUSED,
        FAILED
    }

    public interface DownloadListener {
        void onDownloadStateChanged(int surahNumber, String reciterId, State state);
        void onDownloadProgress(int surahNumber, String reciterId, int downloadedAyahs, int totalAyahs, int progressPercentage);
        void onDownloadCompleted(int surahNumber, String reciterId);
        void onDownloadFailed(int surahNumber, String reciterId, String errorMessage);
    }

    public interface ScanCallback {
        void onScanCompleted(List<DownloadItem> items, long totalCompletedSizeBytes, int completedCount, int inProgressCount, int incompleteFailedCount);
    }

    private static volatile DownloadManager instance;

    private final Map<String, AtomicBoolean> activeCancellationMap = new ConcurrentHashMap<>();
    private final Map<String, Call<ResponseBody>> activeCallMap = new ConcurrentHashMap<>();
    private final List<DownloadListener> listeners = new ArrayList<>();

    private DownloadManager() {}

    public static DownloadManager getInstance() {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager();
                }
            }
        }
        return instance;
    }

    public void addListener(DownloadListener listener) {
        synchronized (listeners) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(DownloadListener listener) {
        synchronized (listeners) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }
    }

    private String getJobKey(int surahNumber, String reciterId) {
        return reciterId + "_" + surahNumber;
    }

    public boolean isSurahDownloading(int surahNumber, String reciterId) {
        String jobKey = getJobKey(surahNumber, reciterId);
        AtomicBoolean isCanceled = activeCancellationMap.get(jobKey);
        return isCanceled != null && !isCanceled.get();
    }

    public static boolean isAudioFileValid(File file) {
        return file != null && file.exists() && file.isFile() && file.length() >= MIN_VALID_AUDIO_BYTES;
    }

    public File getSurahDirectory(Context context, String reciterId) {
        if (context == null || reciterId == null || reciterId.isEmpty()) return null;
        File folder = new File(context.getExternalFilesDir(null), "QuranAudio/" + reciterId);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public boolean isSurahDownloaded(Context context, int surahNumber, String reciterId, String englishName, int expectedAyahCount) {
        if (context == null || reciterId == null || expectedAyahCount <= 0) return false;

        File folder = getSurahDirectory(context, reciterId);
        if (folder == null || !folder.exists()) return false;

        String engName = (englishName != null && !englishName.isEmpty()) ? englishName : "surah";

        for (int i = 1; i <= expectedAyahCount; i++) {
            File audioFile = new File(folder, engName + "_ayah_" + i + ".mp3");
            if (!isAudioFileValid(audioFile)) {
                return false;
            }
        }
        return true;
    }

    public void startOrResumeSurahDownload(Context context, Reciter reciter, SoraDetails soraDetails) {
        if (context == null || reciter == null || soraDetails == null) {
            notifyFailed(0, "", "بيانات التحميل غير صالحة");
            return;
        }

        int surahNumber = soraDetails.getNumber();
        String reciterId = reciter.getIdentifier();
        List<Ayah> ayahs = soraDetails.getAyahs();

        if (ayahs == null || ayahs.isEmpty()) {
            notifyFailed(surahNumber, reciterId, "قائمة الآيات فارغة");
            return;
        }

        String jobKey = getJobKey(surahNumber, reciterId);

        if (isSurahDownloading(surahNumber, reciterId)) {
            Log.d(TAG, "Surah download already in progress: " + jobKey);
            return;
        }

        AtomicBoolean cancellationFlag = new AtomicBoolean(false);
        activeCancellationMap.put(jobKey, cancellationFlag);

        notifyStateChanged(surahNumber, reciterId, State.DOWNLOADING);

        AppExecutors.getInstance().diskIO().execute(() -> {
            File folder = getSurahDirectory(context, reciterId);
            if (folder == null) {
                activeCancellationMap.remove(jobKey);
                notifyFailed(surahNumber, reciterId, "تعذر إنشاء مجلد حفظ التلاوة");
                return;
            }

            int totalAyahs = ayahs.size();
            int downloadedCount = 0;
            String engName = (soraDetails.getEnglishName() != null && !soraDetails.getEnglishName().isEmpty())
                    ? soraDetails.getEnglishName() : "surah";

            for (int i = 0; i < totalAyahs; i++) {
                if (cancellationFlag.get()) {
                    Log.d(TAG, "Download canceled for " + jobKey);
                    activeCancellationMap.remove(jobKey);
                    notifyStateChanged(surahNumber, reciterId, State.PAUSED);
                    return;
                }

                Ayah ayah = ayahs.get(i);
                File finalFile = new File(folder, engName + "_ayah_" + ayah.getNumberInSurah() + ".mp3");

                if (isAudioFileValid(finalFile)) {
                    downloadedCount++;
                    int progress = (int) ((downloadedCount / (float) totalAyahs) * 100);
                    notifyProgress(surahNumber, reciterId, downloadedCount, totalAyahs, progress);
                    continue;
                }

                boolean success = downloadSingleAyahSync(jobKey, ayah.getAudio(), finalFile, cancellationFlag);
                if (success) {
                    downloadedCount++;
                    int progress = (int) ((downloadedCount / (float) totalAyahs) * 100);
                    notifyProgress(surahNumber, reciterId, downloadedCount, totalAyahs, progress);
                } else {
                    if (cancellationFlag.get()) {
                        activeCancellationMap.remove(jobKey);
                        notifyStateChanged(surahNumber, reciterId, State.PAUSED);
                    } else {
                        activeCancellationMap.remove(jobKey);
                        notifyFailed(surahNumber, reciterId, "حدث خطأ أثناء تنزيل الآية رقم " + ayah.getNumberInSurah());
                    }
                    return;
                }
            }

            activeCancellationMap.remove(jobKey);

            saveSurahToRoomSync(context.getApplicationContext(), reciter, soraDetails);

            notifyCompleted(surahNumber, reciterId);
        });
    }

    private boolean downloadSingleAyahSync(String jobKey, String audioUrl, File finalFile, AtomicBoolean cancellationFlag) {
        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            return false;
        }

        File partFile = new File(finalFile.getAbsolutePath() + ".part");
        long existingSize = (partFile.exists()) ? partFile.length() : 0L;

        Call<ResponseBody> call;
        if (existingSize > 0) {
            call = RetrofitClient.getInstance().getApi().downloadFileWithRange(audioUrl, "bytes=" + existingSize + "-");
        } else {
            call = RetrofitClient.getInstance().getApi().downloadFileWithDynamicUrl(audioUrl);
        }

        activeCallMap.put(jobKey, call);

        try {
            Response<ResponseBody> response = call.execute();
            activeCallMap.remove(jobKey);

            if (cancellationFlag.get()) {
                return false;
            }

            if (response.isSuccessful() && response.body() != null) {
                int statusCode = response.code();
                boolean isAppend = (statusCode == 206 && existingSize > 0);

                String contentType = response.headers().get("Content-Type");
                if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                    Log.e(TAG, "Rejected download: Content-Type is HTML text");
                    if (partFile.exists()) partFile.delete();
                    return false;
                }

                boolean written = writeStreamToFile(response.body(), partFile, isAppend, cancellationFlag);
                if (!written || cancellationFlag.get()) {
                    return false;
                }

                if (isAudioFileValid(partFile)) {
                    if (finalFile.exists()) {
                        finalFile.delete();
                    }
                    boolean renamed = partFile.renameTo(finalFile);
                    if (renamed && isAudioFileValid(finalFile)) {
                        return true;
                    }
                }
            } else {
                if (response.errorBody() != null) {
                    response.errorBody().close();
                }
                if (response.code() == 416) {
                    if (partFile.exists()) partFile.delete();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Sync download IOException for " + finalFile.getName(), e);
        } finally {
            activeCallMap.remove(jobKey);
        }

        return false;
    }

    private boolean writeStreamToFile(ResponseBody body, File targetFile, boolean append, AtomicBoolean cancellationFlag) {
        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = new FileOutputStream(targetFile, append)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (cancellationFlag.get()) {
                    outputStream.flush();
                    return false;
                }
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "IOException writing audio stream to " + targetFile.getName(), e);
            return false;
        }
    }

    public void pauseOrCancelDownload(int surahNumber, String reciterId) {
        String jobKey = getJobKey(surahNumber, reciterId);

        AtomicBoolean cancellationFlag = activeCancellationMap.get(jobKey);
        if (cancellationFlag != null) {
            cancellationFlag.set(true);
        }

        Call<ResponseBody> call = activeCallMap.get(jobKey);
        if (call != null) {
            call.cancel();
            activeCallMap.remove(jobKey);
        }

        notifyStateChanged(surahNumber, reciterId, State.PAUSED);
    }

    public void deleteDownloadedSurah(Context context, int surahNumber, String reciterId, String englishName) {
        if (context == null || reciterId == null) return;

        pauseOrCancelDownload(surahNumber, reciterId);

        AppExecutors.getInstance().diskIO().execute(() -> {
            File folder = getSurahDirectory(context, reciterId);
            if (folder != null && folder.exists()) {
                String engName = (englishName != null && !englishName.isEmpty()) ? englishName : "surah";

                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().startsWith(engName + "_ayah_")) {
                            file.delete();
                        }
                    }
                }
            }

            QuranDatabase.getInstance(context.getApplicationContext()).quranDao().deleteSurah(surahNumber, reciterId);
            QuranDatabase.getInstance(context.getApplicationContext()).quranDao().deleteUnusedReciters();

            notifyStateChanged(surahNumber, reciterId, State.IDLE);
        });
    }

    public void deleteAllDownloadedSurahs(Context context, Runnable onComplete) {
        if (context == null) return;

        for (String jobKey : activeCancellationMap.keySet()) {
            AtomicBoolean cancellationFlag = activeCancellationMap.get(jobKey);
            if (cancellationFlag != null) cancellationFlag.set(true);
        }
        for (Call<ResponseBody> call : activeCallMap.values()) {
            if (call != null) call.cancel();
        }
        activeCancellationMap.clear();
        activeCallMap.clear();

        AppExecutors.getInstance().diskIO().execute(() -> {
            File quranFolder = new File(context.getExternalFilesDir(null), "QuranAudio");
            if (quranFolder.exists()) {
                deleteFileOrDirectory(quranFolder);
            }

            try {
                QuranDatabase.getInstance(context.getApplicationContext()).clearAllTables();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing Room database", e);
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (onComplete != null) onComplete.run();
            });
        });
    }

    private void deleteFileOrDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileOrDirectory(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    public void scanAllDownloadItems(Context context, ScanCallback callback) {
        if (context == null || callback == null) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<DownloadItem> items = new ArrayList<>();
            long totalCompletedSize = 0L;
            int completedCount = 0;
            int inProgressCount = 0;
            int incompleteFailedCount = 0;

            Context appContext = context.getApplicationContext();
            List<ReciterEntity> reciterEntities = QuranDatabase.getInstance(appContext).quranDao().getDownloadedReciters();

            Map<String, DownloadItem> itemMap = new HashMap<>();

            for (ReciterEntity reciterEntity : reciterEntities) {
                String reciterId = reciterEntity.getId();
                List<SurahEntity> surahEntities = QuranDatabase.getInstance(appContext).quranDao().getDownloadedSurahsByReciter(reciterId);

                for (SurahEntity surahEntity : surahEntities) {
                    String engName = surahEntity.getEnglishName();
                    int surahNum = surahEntity.getSurahNumber();
                    String key = reciterId + "_" + surahNum;

                    DownloadItem item = new DownloadItem(surahNum, surahEntity.getSurahName(), engName, reciterId, reciterEntity.getName());

                    Reciter reciterModel = new Reciter();
                    reciterModel.setIdentifier(reciterId);
                    reciterModel.setName(reciterEntity.getName());
                    item.setReciterModel(reciterModel);

                    SoraDetails soraDetails = new SoraDetails();
                    soraDetails.setNumber(surahNum);
                    soraDetails.setName(surahEntity.getSurahName());
                    soraDetails.setEnglishName(engName);

                    List<AyahEntity> ayahEntities = QuranDatabase.getInstance(appContext).quranDao().getAyahsBySurahAndReciter(surahNum, reciterId);
                    List<Ayah> ayahs = new ArrayList<>();
                    int downloadedAyahs = 0;
                    long surahSize = 0L;

                    File folder = getSurahDirectory(appContext, reciterId);

                    if (!ayahEntities.isEmpty()) {
                        item.setTotalAyahs(ayahEntities.size());
                        for (AyahEntity entity : ayahEntities) {
                            Ayah ayah = new Ayah();
                            ayah.setNumber(entity.getNumber());
                            ayah.setNumberInSurah(entity.getNumberInSurah());
                            ayah.setText(entity.getText());
                            ayah.setAudio(entity.getAudio());
                            ayahs.add(ayah);

                            File audioFile = new File(folder, engName + "_ayah_" + entity.getNumberInSurah() + ".mp3");
                            if (isAudioFileValid(audioFile)) {
                                downloadedAyahs++;
                                surahSize += audioFile.length();
                            }
                        }
                    }
                    soraDetails.setAyahs(ayahs);
                    item.setSoraDetailsModel(soraDetails);

                    item.setDownloadedAyahs(downloadedAyahs);
                    item.setTotalSizeBytes(surahSize);

                    if (isSurahDownloading(surahNum, reciterId)) {
                        item.setState(State.DOWNLOADING);
                        inProgressCount++;
                    } else if (downloadedAyahs > 0 && downloadedAyahs == ayahEntities.size()) {
                        item.setState(State.COMPLETED);
                        item.setProgressPercentage(100);
                        completedCount++;
                        totalCompletedSize += surahSize;
                    } else if (downloadedAyahs > 0) {
                        item.setState(State.PAUSED);
                        int progress = (item.getTotalAyahs() > 0) ? (int) ((downloadedAyahs / (float) item.getTotalAyahs()) * 100) : 0;
                        item.setProgressPercentage(progress);
                        incompleteFailedCount++;
                    } else {
                        item.setState(State.FAILED);
                        incompleteFailedCount++;
                    }

                    itemMap.put(key, item);
                }
            }

            File audioDir = new File(appContext.getExternalFilesDir(null), "QuranAudio");
            if (audioDir.exists() && audioDir.isDirectory()) {
                File[] reciterDirs = audioDir.listFiles();
                if (reciterDirs != null) {
                    for (File rDir : reciterDirs) {
                        if (rDir.isDirectory()) {
                            String reciterId = rDir.getName();
                            File[] files = rDir.listFiles();
                            if (files != null) {
                                Map<String, List<File>> surahFilesMap = new HashMap<>();
                                for (File file : files) {
                                    String name = file.getName();
                                    if (name.contains("_ayah_")) {
                                        String prefix = name.substring(0, name.indexOf("_ayah_"));
                                        List<File> list = surahFilesMap.get(prefix);
                                        if (list == null) {
                                            list = new ArrayList<>();
                                            surahFilesMap.put(prefix, list);
                                        }
                                        list.add(file);
                                    }
                                }

                                for (Map.Entry<String, List<File>> entry : surahFilesMap.entrySet()) {
                                    String engName = entry.getKey();
                                    List<File> fList = entry.getValue();

                                    long sizeSum = 0L;
                                    int validMp3Count = 0;
                                    boolean hasPartFiles = false;

                                    for (File f : fList) {
                                        if (f.getName().endsWith(".part")) {
                                            hasPartFiles = true;
                                            sizeSum += f.length();
                                        } else if (isAudioFileValid(f)) {
                                            validMp3Count++;
                                            sizeSum += f.length();
                                        }
                                    }

                                    boolean alreadyTracked = false;
                                    for (DownloadItem existingItem : itemMap.values()) {
                                        if (existingItem.getReciterId().equals(reciterId) && existingItem.getEnglishName().equals(engName)) {
                                            alreadyTracked = true;
                                            break;
                                        }
                                    }

                                    if (!alreadyTracked && !fList.isEmpty()) {
                                        DownloadItem untrackedItem = new DownloadItem(0, engName, engName, reciterId, reciterId);
                                        untrackedItem.setTotalSizeBytes(sizeSum);
                                        untrackedItem.setDownloadedAyahs(validMp3Count);
                                        untrackedItem.setTotalAyahs(Math.max(validMp3Count, fList.size()));

                                        if (isSurahDownloading(0, reciterId)) {
                                            untrackedItem.setState(State.DOWNLOADING);
                                            inProgressCount++;
                                        } else if (hasPartFiles) {
                                            untrackedItem.setState(State.PAUSED);
                                            incompleteFailedCount++;
                                        } else if (validMp3Count > 0) {
                                            untrackedItem.setState(State.COMPLETED);
                                            completedCount++;
                                            totalCompletedSize += sizeSum;
                                        } else {
                                            untrackedItem.setState(State.FAILED);
                                            incompleteFailedCount++;
                                        }
                                        itemMap.put(reciterId + "_" + engName, untrackedItem);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            List<DownloadItem> resultList = new ArrayList<>(itemMap.values());
            final long finalTotalCompletedSize = totalCompletedSize;
            final int finalCompletedCount = completedCount;
            final int finalInProgressCount = inProgressCount;
            final int finalIncompleteFailedCount = incompleteFailedCount;

            AppExecutors.getInstance().mainThread().execute(() -> {
                callback.onScanCompleted(resultList, finalTotalCompletedSize, finalCompletedCount, finalInProgressCount, finalIncompleteFailedCount);
            });
        });
    }

    private void saveSurahToRoomSync(Context appContext, Reciter reciter, SoraDetails soraDetails) {
        try {
            ReciterEntity reciterEntity = new ReciterEntity(reciter.getIdentifier(), reciter.getName());
            QuranDatabase.getInstance(appContext).quranDao().insertReciter(reciterEntity);

            SurahEntity surahEntity = new SurahEntity(
                    soraDetails.getNumber(),
                    soraDetails.getName(),
                    reciterEntity.getId(),
                    soraDetails.getEnglishName()
            );

            long surahId = QuranDatabase.getInstance(appContext).quranDao().insertSurah(surahEntity);
            if (surahId == -1) {
                SurahEntity existing = QuranDatabase.getInstance(appContext).quranDao().getSurah(soraDetails.getNumber(), reciter.getIdentifier());
                if (existing != null) {
                    surahId = existing.getId();
                }
            }

            if (surahId > 0 && soraDetails.getAyahs() != null) {
                List<AyahEntity> entities = new ArrayList<>();
                for (Ayah ayah : soraDetails.getAyahs()) {
                    entities.add(new AyahEntity(
                            ayah.getNumber() != null ? ayah.getNumber() : 0,
                            ayah.getNumberInSurah() != null ? ayah.getNumberInSurah() : 0,
                            ayah.getText() != null ? ayah.getText() : "",
                            ayah.getAudio() != null ? ayah.getAudio() : "",
                            (int) surahId
                    ));
                }
                QuranDatabase.getInstance(appContext).quranDao().insertAyahs(entities);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving surah to Room DB", e);
        }
    }

    private void notifyStateChanged(int surahNumber, String reciterId, State state) {
        AppExecutors.getInstance().mainThread().execute(() -> {
            synchronized (listeners) {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadStateChanged(surahNumber, reciterId, state);
                }
            }
        });
    }

    private void notifyProgress(int surahNumber, String reciterId, int downloaded, int total, int progress) {
        AppExecutors.getInstance().mainThread().execute(() -> {
            synchronized (listeners) {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadProgress(surahNumber, reciterId, downloaded, total, progress);
                }
            }
        });
    }

    private void notifyCompleted(int surahNumber, String reciterId) {
        AppExecutors.getInstance().mainThread().execute(() -> {
            synchronized (listeners) {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadStateChanged(surahNumber, reciterId, State.COMPLETED);
                    listener.onDownloadCompleted(surahNumber, reciterId);
                }
            }
        });
    }

    private void notifyFailed(int surahNumber, String reciterId, String errorMessage) {
        AppExecutors.getInstance().mainThread().execute(() -> {
            synchronized (listeners) {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadStateChanged(surahNumber, reciterId, State.FAILED);
                    listener.onDownloadFailed(surahNumber, reciterId, errorMessage);
                }
            }
        });
    }
}
