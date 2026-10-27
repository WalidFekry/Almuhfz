package com.walid.almuhfz.notifications_messages.adapter;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.walid.almuhfz.R;
import com.walid.almuhfz.db.QuranDatabase;
import com.walid.almuhfz.notifications_messages.db.SavedNotificationEntity;
import com.walid.almuhfz.notifications_messages.model.NotificationModel;
import com.walid.almuhfz.notifications_messages.utils.QuoteImageGenerator;
import com.walid.almuhfz.utils.AppExecutors;

import net.steamcrafted.materialiconlib.MaterialIconView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationModel> list;

    public NotificationAdapter(List<NotificationModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_message, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotificationViewHolder holder, int position) {
        if (list == null || position < 0 || position >= list.size()) return;

        NotificationModel notification = list.get(position);
        if (notification == null) return;

        Context itemContext = holder.itemView.getContext();

        String rawTitle = (notification.getTitle() != null) ? notification.getTitle() : "";
        holder.title.setText(rawTitle.replace("_", "\n"));
        holder.messageNumber.setText(String.valueOf(notification.getId()));

        String timeText = "2025-02-14";
        if (notification.getCreatedAt() != null && !notification.getCreatedAt().isEmpty()) {
            String[] parts = notification.getCreatedAt().split(" ");
            if (parts.length > 0) {
                timeText = parts[0];
            }
        }
        final String finalTimeText = timeText;

        holder.messageTime.setText(finalTimeText);

        checkBookmarkStatus(holder, itemContext, notification.getId());

        setClickListener(holder.btnDownloadImage, v -> QuoteImageGenerator.saveQuoteImageToGallery(itemContext, rawTitle, notification.getId(), finalTimeText));
        setClickListener(holder.btnShareImage, v -> QuoteImageGenerator.shareQuoteImage(itemContext, rawTitle, notification.getId(), finalTimeText));
        setClickListener(holder.btnBookmarkMessage, v -> toggleSaveNotification(holder, itemContext, notification, finalTimeText));

        setClickListener(holder.shareButton, v -> shareNotification(itemContext, notification));
        setClickListener(holder.starButton, v -> openLink(itemContext, "https://play.google.com/store/apps/details?id=com.walid.almuhfz"));
        setClickListener(holder.whatsappButton, v -> shareOnWhatsApp(itemContext, rawTitle));
        setClickListener(holder.copyButton, v -> copyToClipboard(itemContext, rawTitle));
        setClickListener(holder.facebookButton, v -> shareOnFacebook(itemContext, rawTitle));
    }

    private void checkBookmarkStatus(NotificationViewHolder holder, Context context, int notificationId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SavedNotificationEntity saved = QuranDatabase.getInstance(context.getApplicationContext()).quranDao().getSavedNotificationById(notificationId);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (holder.btnBookmarkMessage != null) {
                    if (saved != null) {
                        holder.btnBookmarkMessage.setText("محفوظ 🔖");
                    } else {
                        holder.btnBookmarkMessage.setText("حفظ 🔖");
                    }
                }
            });
        });
    }

    private void toggleSaveNotification(NotificationViewHolder holder, Context context, NotificationModel notification, String dateText) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SavedNotificationEntity existing = QuranDatabase.getInstance(context.getApplicationContext()).quranDao().getSavedNotificationById(notification.getId());
            if (existing != null) {
                QuranDatabase.getInstance(context.getApplicationContext()).quranDao().deleteSavedNotification(notification.getId());
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (holder.btnBookmarkMessage != null) holder.btnBookmarkMessage.setText("حفظ 🔖");
                    Toast.makeText(context, "تم إزالة الرسالة من المحفوظات", Toast.LENGTH_SHORT).show();
                });
            } else {
                SavedNotificationEntity entity = new SavedNotificationEntity(
                        notification.getId(),
                        notification.getTitle(),
                        dateText,
                        System.currentTimeMillis()
                );
                QuranDatabase.getInstance(context.getApplicationContext()).quranDao().insertSavedNotification(entity);
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (holder.btnBookmarkMessage != null) holder.btnBookmarkMessage.setText("محفوظ 🔖");
                    Toast.makeText(context, "تم حفظ الرسالة بنجاح 🔖", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setClickListener(View view, View.OnClickListener listener) {
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    private String buildSharedText(String text) {
        String cleanText = (text != null) ? text.replace("_", "\n").trim() : "";
        return cleanText + "\n\n🌸 من تطبيق المُحفظ للقرآن الكريم 👇\nhttps://play.google.com/store/apps/details?id=com.walid.almuhfz";
    }

    private void shareOnFacebook(Context context, String text) {
        if (context == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setPackage("com.facebook.katana");
        intent.putExtra(Intent.EXTRA_TEXT, buildSharedText(text));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "تطبيق فيسبوك غير مثبت!", Toast.LENGTH_LONG).show();
        }
    }

    private void shareNotification(Context context, NotificationModel notification) {
        if (context == null || notification == null) return;
        String rawTitle = (notification.getTitle() != null) ? notification.getTitle() : "";
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "تطبيق المُحفظ للقرآن الكريم 🌸");
        sendIntent.putExtra(Intent.EXTRA_TEXT, buildSharedText(rawTitle));
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الاقتباس عبر..."));
    }

    private void openLink(Context context, String url) {
        if (context == null || url == null) return;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private void shareOnWhatsApp(Context context, String text) {
        if (context == null) return;
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, buildSharedText(text));
        try {
            context.startActivity(whatsappIntent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, "تطبيق واتساب غير مثبت!", Toast.LENGTH_LONG).show();
        }
    }

    private void copyToClipboard(Context context, String text) {
        if (context == null) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("المُحفظ", buildSharedText(text));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "تم نسخ النص والمصدر بنجاح 📋", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return (list != null) ? list.size() : 0;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialIconView whatsappButton, copyButton, facebookButton;
        TextView title;
        LottieAnimationView starButton, shareButton;
        AppCompatTextView messageNumber, messageTime;
        Button btnDownloadImage, btnShareImage, btnBookmarkMessage;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            shareButton = itemView.findViewById(R.id.sharequran);
            starButton = itemView.findViewById(R.id.starquran);
            copyButton = itemView.findViewById(R.id.copyquran);
            whatsappButton = itemView.findViewById(R.id.qumsgwhats);
            messageNumber = itemView.findViewById(R.id.message_number);
            messageTime = itemView.findViewById(R.id.message_time);
            facebookButton = itemView.findViewById(R.id.facebook_share);

            btnDownloadImage = itemView.findViewById(R.id.btnDownloadImage);
            btnShareImage = itemView.findViewById(R.id.btnShareImage);
            btnBookmarkMessage = itemView.findViewById(R.id.btnBookmarkMessage);
        }
    }
}
