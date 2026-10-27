# Add project specific ProGuard rules here.

# Preserve SourceFile and LineNumberTable for crash stacktrace de-obfuscation in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- GSON & Retrofit Models ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Keep All Model & Entity Classes
-keep class com.walid.almuhfz.learning.models.** { *; }
-keep class com.walid.almuhfz.notifications_messages.model.** { *; }
-keep class com.walid.almuhfz.db.entity.** { *; }
-keep class com.walid.almuhfz.notifications_messages.db.** { *; }
-keep class com.walid.almuhfz.download.DownloadItem { *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Retrofit & OkHttp ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- JcPlayer ---
-keep class com.example.jean.jcplayer.** { *; }

# --- Lottie ---
-keep class com.airbnb.lottie.** { *; }

# --- Firebase & Google Play Services ---
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
