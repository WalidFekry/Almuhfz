package com.walid.almuhfz.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "bookmarks")
public class BookmarkEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int surahNumber;
    private String surahName;
    private int numberInSurah;
    private String ayahText;
    private String reciterId;
    private String note;
    private String category; // e.g. "حفظ", "مراجعة", "إتقان"
    private long timestamp;

    public BookmarkEntity(int surahNumber, String surahName, int numberInSurah, String ayahText, String reciterId, String note, String category, long timestamp) {
        this.surahNumber = surahNumber;
        this.surahName = (surahName != null) ? surahName : "";
        this.numberInSurah = numberInSurah;
        this.ayahText = (ayahText != null) ? ayahText : "";
        this.reciterId = (reciterId != null) ? reciterId : "";
        this.note = (note != null) ? note : "";
        this.category = (category != null) ? category : "حفظ";
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSurahNumber() {
        return surahNumber;
    }

    public void setSurahNumber(int surahNumber) {
        this.surahNumber = surahNumber;
    }

    public String getSurahName() {
        return surahName;
    }

    public void setSurahName(String surahName) {
        this.surahName = surahName;
    }

    public int getNumberInSurah() {
        return numberInSurah;
    }

    public void setNumberInSurah(int numberInSurah) {
        this.numberInSurah = numberInSurah;
    }

    public String getAyahText() {
        return ayahText;
    }

    public void setAyahText(String ayahText) {
        this.ayahText = ayahText;
    }

    public String getReciterId() {
        return reciterId;
    }

    public void setReciterId(String reciterId) {
        this.reciterId = reciterId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
