package com.walid.almuhfz.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ayahs",
        foreignKeys = @ForeignKey(
                entity = SurahEntity.class,
                parentColumns = "id",
                childColumns = "surahId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("surahId")
)
public class AyahEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int number;
    private int numberInSurah;
    private String text;
    private String audio;
    private int surahId;

    public AyahEntity(int number, int numberInSurah, String text, String audio, int surahId) {
        this.number = number;
        this.numberInSurah = numberInSurah;
        this.text = text;
        this.audio = audio;
        this.surahId = surahId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumberInSurah() {
        return numberInSurah;
    }

    public void setNumberInSurah(int numberInSurah) {
        this.numberInSurah = numberInSurah;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public int getSurahId() {
        return surahId;
    }

    public void setSurahId(int surahId) {
        this.surahId = surahId;
    }
}
