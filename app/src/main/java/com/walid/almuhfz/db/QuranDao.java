package com.walid.almuhfz.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.db.entity.BookmarkEntity;
import com.walid.almuhfz.db.entity.ReciterEntity;
import com.walid.almuhfz.db.entity.SurahEntity;

import java.util.List;

@Dao
public interface QuranDao {

    @Query("SELECT * FROM surahs WHERE surahNumber = :surahNumber AND reciterId = :reciterId LIMIT 1")
    SurahEntity getSurah(int surahNumber, String reciterId);

    @Query("SELECT * FROM ayahs WHERE surahId = (SELECT id FROM surahs WHERE surahNumber = :surahNumber AND reciterId = :reciterId LIMIT 1) ORDER BY numberInSurah")
    List<AyahEntity> getAyahsBySurahAndReciter(int surahNumber, String reciterId);

    @Query("SELECT * FROM ayahs WHERE surahId = :surahNumber ORDER BY numberInSurah")
    List<AyahEntity> getAyahsBySurah(int surahNumber);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertReciter(ReciterEntity reciter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSurah(SurahEntity surah);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAyahs(List<AyahEntity> ayahs);

    @Query("DELETE FROM surahs WHERE surahNumber = :surahNumber AND reciterId = :reciterId")
    void deleteSurah(int surahNumber, String reciterId);

    @Query("DELETE FROM reciters WHERE id NOT IN (SELECT DISTINCT reciterId FROM surahs)")
    void deleteUnusedReciters();

    @Query("SELECT DISTINCT r.* FROM reciters r INNER JOIN surahs s ON r.id = s.reciterId")
    List<ReciterEntity> getDownloadedReciters();

    @Query("SELECT DISTINCT * FROM surahs WHERE reciterId = :reciterId")
    List<SurahEntity> getDownloadedSurahsByReciter(String reciterId);

    // --- BOOKMARKS ---
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    List<BookmarkEntity> getAllBookmarks();

    @Query("SELECT * FROM bookmarks WHERE category = :category ORDER BY timestamp DESC")
    List<BookmarkEntity> getBookmarksByCategory(String category);

    @Query("SELECT * FROM bookmarks WHERE surahNumber = :surahNumber AND numberInSurah = :numberInSurah LIMIT 1")
    BookmarkEntity getBookmarkByAyah(int surahNumber, int numberInSurah);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBookmark(BookmarkEntity bookmark);

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    void deleteBookmark(int bookmarkId);

    @Query("DELETE FROM bookmarks WHERE surahNumber = :surahNumber AND numberInSurah = :numberInSurah")
    void deleteBookmarkByAyah(int surahNumber, int numberInSurah);

    // --- SEARCH ---
    @Query("SELECT * FROM ayahs WHERE text LIKE '%' || :query || '%' GROUP BY text ORDER BY numberInSurah LIMIT 100")
    List<AyahEntity> searchAyahsByText(String query);

    @Query("SELECT * FROM ayahs ORDER BY numberInSurah")
    List<AyahEntity> getAllAyahsForSearch();
}
