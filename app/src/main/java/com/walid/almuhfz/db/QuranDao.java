package com.walid.almuhfz.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.ReciterEntity;
import com.walid.almuhfz.db.entity.SurahEntity;

import java.util.List;

@Dao
public interface QuranDao {
    @Query("SELECT * FROM ayahs WHERE surahId = :surahNumber ORDER BY numberInSurah")
    List<AyahEntity> getAyahsBySurah(int surahNumber);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertReciter(ReciterEntity reciter);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSurah(SurahEntity surah);

    @Insert
    void insertAyahs(List<AyahEntity> ayahs);

    @Query("SELECT DISTINCT r.* FROM reciters r INNER JOIN surahs s ON r.id = s.reciterId")
    List<ReciterEntity> getDownloadedReciters();

    @Query("SELECT DISTINCT * FROM surahs WHERE reciterId = :reciterId")
    List<SurahEntity> getDownloadedSurahsByReciter(String reciterId);

}
