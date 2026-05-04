package com.walid.almuhfz.learning.models;

import androidx.annotation.Keep;

@Keep
public class Sora {

    private int startPageNum;
    private String name;

    private String englishName;

    public Sora() {
    }

    public Sora(int startPageNum, String name) {
        this.startPageNum = startPageNum;
        this.name = name;
    }

    public Sora(int startPageNum, String name, String englishName) {
        this.startPageNum = startPageNum;
        this.name = name;
        this.englishName = englishName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public int getStartPageNum() {
        return startPageNum;
    }

    public void setStartPageNum(int startPageNum) {
        this.startPageNum = startPageNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
