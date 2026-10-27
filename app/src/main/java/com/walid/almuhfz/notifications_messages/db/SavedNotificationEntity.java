package com.walid.almuhfz.notifications_messages.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "saved_notifications")
public class SavedNotificationEntity implements Serializable {

    @PrimaryKey
    private int id;

    private String title;
    private String createdAt;
    private long savedTimestamp;

    public SavedNotificationEntity(int id, String title, String createdAt, long savedTimestamp) {
        this.id = id;
        this.title = (title != null) ? title : "";
        this.createdAt = (createdAt != null) ? createdAt : "";
        this.savedTimestamp = savedTimestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public long getSavedTimestamp() {
        return savedTimestamp;
    }

    public void setSavedTimestamp(long savedTimestamp) {
        this.savedTimestamp = savedTimestamp;
    }
}
