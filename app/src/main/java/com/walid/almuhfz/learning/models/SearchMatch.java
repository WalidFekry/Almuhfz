package com.walid.almuhfz.learning.models;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Keep
public class SearchMatch implements Serializable {

    @SerializedName("number")
    @Expose
    private Integer number;

    @SerializedName("text")
    @Expose
    private String text;

    @SerializedName("numberInSurah")
    @Expose
    private Integer numberInSurah;

    @SerializedName("surah")
    @Expose
    private SoraDetails surah;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getNumberInSurah() {
        return numberInSurah;
    }

    public void setNumberInSurah(Integer numberInSurah) {
        this.numberInSurah = numberInSurah;
    }

    public SoraDetails getSurah() {
        return surah;
    }

    public void setSurah(SoraDetails surah) {
        this.surah = surah;
    }
}
