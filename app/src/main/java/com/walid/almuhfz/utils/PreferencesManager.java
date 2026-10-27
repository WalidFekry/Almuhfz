package com.walid.almuhfz.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREF_NAME = "app_selections_prefs";
    private static final String KEY_RECITER_ID = "key_reciter_id";
    private static final String KEY_RECITER_NAME = "key_reciter_name";
    private static final String KEY_SURAH_NUM = "key_surah_num";
    private static final String KEY_FROM_AYA = "key_from_aya";
    private static final String KEY_TO_AYA = "key_to_aya";
    private static final String KEY_REPEAT_AYA = "key_repeat_aya";
    private static final String KEY_REPEAT_SORA = "key_repeat_sora";

    public static void saveSelectedReciter(Context context, String identifier, String name) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_RECITER_ID, identifier != null ? identifier : "ar.alafasy")
                .putString(KEY_RECITER_NAME, name != null ? name : "مشاري العفاسي")
                .apply();
    }

    public static String getSavedReciterId(Context context) {
        if (context == null) return "ar.alafasy";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_RECITER_ID, "ar.alafasy");
    }

    public static String getSavedReciterName(Context context) {
        if (context == null) return "مشاري العفاسي";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_RECITER_NAME, "مشاري العفاسي");
    }

    public static void saveSelections(Context context, int surahNum, int fromAya, int toAya, int repeatAya, int repeatSora) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_SURAH_NUM, Math.max(1, surahNum))
                .putInt(KEY_FROM_AYA, Math.max(1, fromAya))
                .putInt(KEY_TO_AYA, Math.max(1, toAya))
                .putInt(KEY_REPEAT_AYA, Math.max(1, repeatAya))
                .putInt(KEY_REPEAT_SORA, Math.max(1, repeatSora))
                .apply();
    }

    public static int getSavedSurahNum(Context context) {
        if (context == null) return 1;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_SURAH_NUM, 1);
    }

    public static int getSavedFromAya(Context context) {
        if (context == null) return 1;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_FROM_AYA, 1);
    }

    public static int getSavedToAya(Context context) {
        if (context == null) return 7;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_TO_AYA, 7);
    }

    public static int getSavedRepeatAya(Context context) {
        if (context == null) return 1;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_REPEAT_AYA, 1);
    }

    public static int getSavedRepeatSora(Context context) {
        if (context == null) return 1;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_REPEAT_SORA, 1);
    }
}
