package com.walid.almuhfz.learning.models;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

@Keep
public class SearchData implements Serializable {

    @SerializedName("count")
    @Expose
    private Integer count;

    @SerializedName("matches")
    @Expose
    private List<SearchMatch> matches;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<SearchMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<SearchMatch> matches) {
        this.matches = matches;
    }
}
