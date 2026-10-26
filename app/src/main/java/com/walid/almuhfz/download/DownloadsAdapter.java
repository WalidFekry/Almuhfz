package com.walid.almuhfz.download;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;

import java.util.ArrayList;
import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

    public interface ActionListener {
        void onPlayOfflineClicked(DownloadItem item);
        void onResumeRetryClicked(DownloadItem item);
        void onPauseClicked(DownloadItem item);
        void onDeleteClicked(DownloadItem item);
    }

    private final List<DownloadItem> items = new ArrayList<>();
    private final ActionListener actionListener;

    public DownloadsAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setItems(List<DownloadItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<DownloadItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_surah, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;

        DownloadItem item = items.get(position);
        if (item == null) return;

        Context context = holder.itemView.getContext();

        holder.tvSurahName.setText(item.getSurahName());
        holder.tvReciterName.setText("القارئ: " + item.getReciterName());

        String fileInfoStr = item.getDownloadedAyahs() + "/" + item.getTotalAyahs() + " آية • " + item.getFormattedSize();
        holder.tvFileInfo.setText(fileInfoStr);

        DownloadManager.State state = item.getState();

        holder.itemProgressBar.setVisibility(View.GONE);
        holder.tvErrorMessage.setVisibility(View.GONE);
        holder.btnPlayOffline.setVisibility(View.GONE);
        holder.btnResumeRetry.setVisibility(View.GONE);
        holder.btnPause.setVisibility(View.GONE);
        holder.btnDelete.setVisibility(View.VISIBLE);

        if (state == DownloadManager.State.COMPLETED) {
            holder.tvStateBadge.setText("مكتملة ✔");
            holder.tvStateBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            holder.btnPlayOffline.setVisibility(View.VISIBLE);
        } else if (state == DownloadManager.State.DOWNLOADING) {
            holder.tvStateBadge.setText("جاري التحميل ⏳");
            holder.tvStateBadge.setBackgroundColor(Color.parseColor("#1976D2"));
            holder.itemProgressBar.setVisibility(View.VISIBLE);
            holder.itemProgressBar.setProgress(item.getProgressPercentage());
            holder.btnPause.setVisibility(View.VISIBLE);
        } else if (state == DownloadManager.State.PAUSED) {
            holder.tvStateBadge.setText("متوقفة ⏸");
            holder.tvStateBadge.setBackgroundColor(Color.parseColor("#F57C00"));
            holder.btnResumeRetry.setText("استئناف");
            holder.btnResumeRetry.setVisibility(View.VISIBLE);
        } else if (state == DownloadManager.State.FAILED) {
            holder.tvStateBadge.setText("فشلت ⚠️");
            holder.tvStateBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
            holder.btnResumeRetry.setText("إعادة المحاولة");
            holder.btnResumeRetry.setVisibility(View.VISIBLE);
            if (item.getErrorMessage() != null && !item.getErrorMessage().isEmpty()) {
                holder.tvErrorMessage.setText(item.getErrorMessage());
                holder.tvErrorMessage.setVisibility(View.VISIBLE);
            }
        } else {
            holder.tvStateBadge.setText("غير محملة");
            holder.tvStateBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.gray));
            holder.btnResumeRetry.setText("تحميل");
            holder.btnResumeRetry.setVisibility(View.VISIBLE);
        }

        holder.btnPlayOffline.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onPlayOfflineClicked(item);
        });

        holder.btnResumeRetry.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onResumeRetryClicked(item);
        });

        holder.btnPause.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onPauseClicked(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDeleteClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSurahName, tvReciterName, tvStateBadge, tvFileInfo, tvErrorMessage;
        ProgressBar itemProgressBar;
        Button btnPlayOffline, btnResumeRetry, btnPause, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSurahName = itemView.findViewById(R.id.tvSurahName);
            tvReciterName = itemView.findViewById(R.id.tvReciterName);
            tvStateBadge = itemView.findViewById(R.id.tvStateBadge);
            tvFileInfo = itemView.findViewById(R.id.tvFileInfo);
            tvErrorMessage = itemView.findViewById(R.id.tvErrorMessage);
            itemProgressBar = itemView.findViewById(R.id.itemProgressBar);
            btnPlayOffline = itemView.findViewById(R.id.btnPlayOffline);
            btnResumeRetry = itemView.findViewById(R.id.btnResumeRetry);
            btnPause = itemView.findViewById(R.id.btnPause);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
