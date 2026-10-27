package com.walid.almuhfz.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.BookmarkEntity;
import com.walid.almuhfz.db.entity.ReciterEntity;
import com.walid.almuhfz.db.entity.SurahEntity;

@Database(entities = {AyahEntity.class, SurahEntity.class, ReciterEntity.class, BookmarkEntity.class}, version = 1, exportSchema = false)
public abstract class QuranDatabase extends RoomDatabase {
    private static volatile QuranDatabase INSTANCE;

    public static QuranDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (QuranDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), QuranDatabase.class, "quran_db").build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract QuranDao quranDao();
}
