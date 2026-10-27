package com.walid.almuhfz.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.db.entity.AyahEntity;
import com.walid.almuhfz.learning.models.Sora;
import com.walid.almuhfz.learning.networking.QuranUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public interface ActionListener {
        void onOpenAyahClicked(AyahEntity ayah, String surahName, int surahNumber);
        void onBookmarkClicked(AyahEntity ayah, String surahName, int surahNumber);
    }

    private final List<AyahEntity> items = new ArrayList<>();
    private final ActionListener actionListener;
    private final List<Sora> soraList = QuranUtils.getSoraList();

    public SearchAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setItems(List<AyahEntity> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;

        AyahEntity ayah = items.get(position);
        if (ayah == null) return;

        int surahNum = ayah.getSurahId();
        String surahName = "سورة " + surahNum;
        if (soraList != null && surahNum > 0 && surahNum <= soraList.size()) {
            surahName = soraList.get(surahNum - 1).getName();
        }

        final String finalSurahName = surahName;

        holder.tvSurahAyahTitle.setText("سورة " + finalSurahName + " - الآية " + ayah.getNumberInSurah());
        holder.tvAyahText.setText(ayah.getText());

        holder.btnOpenAyah.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onOpenAyahClicked(ayah, finalSurahName, surahNum);
        });

        holder.btnBookmarkAyah.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onBookmarkClicked(ayah, finalSurahName, surahNum);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSurahAyahTitle, tvAyahText;
        Button btnOpenAyah, btnBookmarkAyah;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSurahAyahTitle = itemView.findViewById(R.id.tvSurahAyahTitle);
            tvAyahText = itemView.findViewById(R.id.tvAyahText);
            btnOpenAyah = itemView.findViewById(R.id.btnOpenAyah);
            btnBookmarkAyah = itemView.findViewById(R.id.btnBookmarkAyah);
        }
    }
}
