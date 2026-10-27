package com.walid.almuhfz.bookmarks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.db.entity.BookmarkEntity;

import java.util.ArrayList;
import java.util.List;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.ViewHolder> {

    public interface ActionListener {
        void onPlayAyahClicked(BookmarkEntity bookmark);
        void onDeleteClicked(BookmarkEntity bookmark);
    }

    private final List<BookmarkEntity> items = new ArrayList<>();
    private final ActionListener actionListener;

    public BookmarksAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setItems(List<BookmarkEntity> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;

        BookmarkEntity bookmark = items.get(position);
        if (bookmark == null) return;

        holder.tvSurahAyahInfo.setText(bookmark.getSurahName() + " - الآية " + bookmark.getNumberInSurah());
        holder.tvCategoryBadge.setText(bookmark.getCategory() != null ? bookmark.getCategory() : "حفظ");
        holder.tvAyahText.setText(bookmark.getAyahText());

        if (bookmark.getNote() != null && !bookmark.getNote().trim().isEmpty()) {
            holder.tvBookmarkNote.setText("ملاحظة: " + bookmark.getNote());
            holder.tvBookmarkNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvBookmarkNote.setVisibility(View.GONE);
        }

        holder.btnPlayAyah.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onPlayAyahClicked(bookmark);
        });

        holder.btnDeleteBookmark.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDeleteClicked(bookmark);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSurahAyahInfo, tvCategoryBadge, tvAyahText, tvBookmarkNote;
        Button btnPlayAyah, btnDeleteBookmark;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSurahAyahInfo = itemView.findViewById(R.id.tvSurahAyahInfo);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvAyahText = itemView.findViewById(R.id.tvAyahText);
            tvBookmarkNote = itemView.findViewById(R.id.tvBookmarkNote);
            btnPlayAyah = itemView.findViewById(R.id.btnPlayAyah);
            btnDeleteBookmark = itemView.findViewById(R.id.btnDeleteBookmark);
        }
    }
}
