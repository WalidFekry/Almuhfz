package com.walid.almuhfz.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "surahs",
        foreignKeys = @ForeignKey(
                entity = ReciterEntity.class,
                parentColumns = "id",
                childColumns = "reciterId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"surahNumber", "reciterId"}, unique = true)
        }
)
public class SurahEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int surahNumber;
    private String surahName;
    private String englishName;
    private String reciterId;

    public SurahEntity(int surahNumber, String surahName, String reciterId, String englishName) {
        this.surahNumber = surahNumber;
        this.surahName = surahName;
        this.reciterId = reciterId;
        this.englishName = englishName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
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

    public String getReciterId() {
        return reciterId;
    }

    public void setReciterId(String reciterId) {
        this.reciterId = reciterId;
    }
}
