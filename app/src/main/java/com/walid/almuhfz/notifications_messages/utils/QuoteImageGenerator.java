package com.walid.almuhfz.notifications_messages.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import com.walid.almuhfz.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class QuoteImageGenerator {

    private static final String TAG = "QuoteImageGenerator";

    public static Bitmap generateQuoteBitmap(Context context, String messageText, int messageNumber, String dateText) {
        int width = 1080;
        int height = 1080;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Background Gradient (Deep Emerald Green)
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, height,
                Color.parseColor("#1A4D2E"),
                Color.parseColor("#0E2516"),
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, bgPaint);

        // 2. Gold Decorative Border Frame
        Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        framePaint.setColor(Color.parseColor("#D4AF37"));
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(8f);

        RectF outerRect = new RectF(36, 36, width - 36, height - 36);
        canvas.drawRoundRect(outerRect, 24, 24, framePaint);

        RectF innerRect = new RectF(48, 48, width - 48, height - 48);
        framePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(innerRect, 18, 18, framePaint);

        // 3. Header Text ("المُحفظ للقرآن الكريم 🌸")
        Typeface jazeeraFont = null;
        try {
            jazeeraFont = ResourcesCompat.getFont(context, R.font.jazeera);
        } catch (Exception ignored) {}

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#FFD54F"));
        headerPaint.setTextSize(40f);
        headerPaint.setTextAlign(Paint.Align.CENTER);
        if (jazeeraFont != null) headerPaint.setTypeface(jazeeraFont);

        canvas.drawText("المُحفظ للقرآن الكريم • المصحف المعلم", width / 2f, 120f, headerPaint);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(Color.parseColor("#44D4AF37"));
        dividerPaint.setStrokeWidth(2f);
        canvas.drawLine(120f, 150f, width - 120f, 150f, dividerPaint);

        // 4. Quote Message Text
        String formattedText = (messageText != null) ? messageText.replace("_", "\n") : "";

        Typeface msgFont = null;
        try {
            msgFont = ResourcesCompat.getFont(context, R.font.messages);
        } catch (Exception ignored) {}

        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(44f);
        if (msgFont != null) textPaint.setTypeface(msgFont);

        int maxTextWidth = width - 180;
        StaticLayout staticLayout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(formattedText, 0, formattedText.length(), textPaint, maxTextWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(14f, 1.2f)
                    .build();
        } else {
            staticLayout = new StaticLayout(formattedText, textPaint, maxTextWidth, Layout.Alignment.ALIGN_CENTER, 1.2f, 14f, false);
        }

        int textHeight = staticLayout.getHeight();
        float startY = (height / 2f) - (textHeight / 2f);

        canvas.save();
        canvas.translate(90f, startY);
        staticLayout.draw(canvas);
        canvas.restore();

        // 5. Footer Line & Info
        canvas.drawLine(120f, height - 160f, width - 120f, height - 160f, dividerPaint);

        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.parseColor("#B0BEC5"));
        footerPaint.setTextSize(30f);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        if (jazeeraFont != null) footerPaint.setTypeface(jazeeraFont);

        String footerText = "رسالة رقم " + messageNumber + " • " + ((dateText != null) ? dateText : "");
        canvas.drawText(footerText, width / 2f, height - 110f, footerPaint);

        footerPaint.setTextSize(26f);
        footerPaint.setColor(Color.parseColor("#FFD54F"));
        canvas.drawText("حمل تطبيق المُحفظ مجاناً من متجر Google Play 📲", width / 2f, height - 65f, footerPaint);

        return bitmap;
    }

    public static boolean saveQuoteImageToGallery(Context context, String messageText, int messageNumber, String dateText) {
        if (context == null) return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3001);
                }
                Toast.makeText(context, "يرجى منح إذن التخزين لحفظ الصور في المعرض 🖼️", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        try {
            Bitmap bitmap = generateQuoteBitmap(context, messageText, messageNumber, dateText);
            String fileName = "Almuhfz_Quote_" + messageNumber + "_" + System.currentTimeMillis() + ".png";

            OutputStream fos = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AlmuhfzQuotes");

                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = context.getContentResolver().openOutputStream(imageUri);
                }
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File albumDir = new File(imagesDir, "AlmuhfzQuotes");
                if (!albumDir.exists()) {
                    albumDir.mkdirs();
                }
                File imageFile = new File(albumDir, fileName);
                fos = new FileOutputStream(imageFile);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(context, "تم حفظ صورة الاقتباس بنجاح في المعرض 🖼️", Toast.LENGTH_SHORT).show();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving quote image", e);
            Toast.makeText(context, "تعذر حفظ الصورة، يرجى إعادة المحاولة.", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public static void shareQuoteImage(Context context, String messageText, int messageNumber, String dateText) {
        if (context == null) return;
        try {
            Bitmap bitmap = generateQuoteBitmap(context, messageText, messageNumber, dateText);
            File cachePath = new File(context.getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }

            File file = new File(cachePath, "quote_share_" + messageNumber + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "رسالة تحفيزية من تطبيق المُحفظ للقرآن الكريم\nحمل التطبيق من متجر بلاي: https://play.google.com/store/apps/details?id=" + context.getPackageName());
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة صورة الاقتباس عبر..."));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing quote image", e);
            Toast.makeText(context, "تعذر مشاركة الصورة، يرجى التحقق من الأذونات.", Toast.LENGTH_SHORT).show();
        }
    }
}
