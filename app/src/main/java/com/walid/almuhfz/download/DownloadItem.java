package com.walid.almuhfz.download;

import com.walid.almuhfz.learning.models.Reciter;
import com.walid.almuhfz.learning.models.SoraDetails;

import java.io.Serializable;
import java.util.Locale;

public class DownloadItem implements Serializable {

    private final int surahNumber;
    private final String surahName;
    private final String englishName;
    private final String reciterId;
    private final String reciterName;

    private int totalAyahs;
    private int downloadedAyahs;
    private long totalSizeBytes;
    private DownloadManager.State state;
    private int progressPercentage;
    private String errorMessage;

    private Reciter reciterModel;
    private SoraDetails soraDetailsModel;

    public DownloadItem(int surahNumber, String surahName, String englishName, String reciterId, String reciterName) {
        this.surahNumber = surahNumber;
        this.surahName = (surahName != null) ? surahName : "";
        this.englishName = (englishName != null) ? englishName : "surah";
        this.reciterId = (reciterId != null) ? reciterId : "";
        this.reciterName = (reciterName != null) ? reciterName : "";
        this.state = DownloadManager.State.IDLE;
    }

    public int getSurahNumber() {
        return surahNumber;
    }

    public String getSurahName() {
        return surahName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getReciterId() {
        return reciterId;
    }

    public String getReciterName() {
        return reciterName;
    }

    public int getTotalAyahs() {
        return totalAyahs;
    }

    public void setTotalAyahs(int totalAyahs) {
        this.totalAyahs = totalAyahs;
    }

    public int getDownloadedAyahs() {
        return downloadedAyahs;
    }

    public void setDownloadedAyahs(int downloadedAyahs) {
        this.downloadedAyahs = downloadedAyahs;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public DownloadManager.State getState() {
        return state;
    }

    public void setState(DownloadManager.State state) {
        this.state = state;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Reciter getReciterModel() {
        return reciterModel;
    }

    public void setReciterModel(Reciter reciterModel) {
        this.reciterModel = reciterModel;
    }

    public SoraDetails getSoraDetailsModel() {
        return soraDetailsModel;
    }

    public void setSoraDetailsModel(SoraDetails soraDetailsModel) {
        this.soraDetailsModel = soraDetailsModel;
    }

    public String getFormattedSize() {
        if (totalSizeBytes <= 0) return "0 MB";
        double sizeInMb = totalSizeBytes / (1024.0 * 1024.0);
        if (sizeInMb < 0.1) {
            double sizeInKb = totalSizeBytes / 1024.0;
            return String.format(Locale.US, "%.1f KB", sizeInKb);
        }
        return String.format(Locale.US, "%.1f MB", sizeInMb);
    }
}
