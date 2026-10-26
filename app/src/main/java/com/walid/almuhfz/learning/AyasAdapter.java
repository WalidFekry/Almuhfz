package com.walid.almuhfz.learning;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.walid.almuhfz.R;
import com.walid.almuhfz.learning.models.Ayah;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AyasAdapter extends RecyclerView.Adapter<AyasAdapter.Holder> {
    private final NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("ar-EG"));
    private int selectedAyaIndex = RecyclerView.NO_POSITION;
    private final List<Ayah> ayahList;
    private final Interaction interaction;

    public AyasAdapter(List<Ayah> ayahList, Interaction interaction) {
        this.ayahList = ayahList;
        this.interaction = interaction;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.aya_item_layout, parent, false);
        return new Holder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        int adapterPosition = holder.getBindingAdapterPosition();

        if (adapterPosition == RecyclerView.NO_POSITION || ayahList == null || adapterPosition >= ayahList.size()) {
            return;
        }

        Ayah currentAyah = ayahList.get(adapterPosition);
        String ayahText = (currentAyah != null && currentAyah.getText() != null) ? currentAyah.getText() : "";
        holder.ayaTextView.setText(nf.format(position + 1) + " - " + ayahText);

        int textColor = (selectedAyaIndex != RecyclerView.NO_POSITION && selectedAyaIndex == adapterPosition) ?
                ContextCompat.getColor(holder.ayaTextView.getContext(), R.color.black) :
                ContextCompat.getColor(holder.ayaTextView.getContext(), R.color.white);
        holder.ayaTextView.setTextColor(textColor);

        holder.ayaTextView.setOnClickListener(view -> {
            int newPosition = holder.getBindingAdapterPosition();

            if (newPosition == RecyclerView.NO_POSITION || ayahList == null || newPosition >= ayahList.size()) {
                return;
            }

            if (interaction != null) {
                interaction.onAyaClicked(ayahList.get(newPosition), newPosition);
            }

            int oldSelectedIndex = selectedAyaIndex;
            selectedAyaIndex = newPosition;

            if (oldSelectedIndex != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldSelectedIndex);
            }
            notifyItemChanged(selectedAyaIndex);
        });
    }

    @Override
    public int getItemCount() {
        return (ayahList != null) ? ayahList.size() : 0;
    }

    public void setSelectedAyaIndex(int newIndex) {
        int oldSelectedIndex = selectedAyaIndex;
        selectedAyaIndex = newIndex;

        if (oldSelectedIndex != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldSelectedIndex);
        }
        if (selectedAyaIndex != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedAyaIndex);
        }
    }

    public interface Interaction {
        void onAyaClicked(Ayah ayah, int position);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView ayaTextView;

        public Holder(@NonNull View itemView) {
            super(itemView);
            ayaTextView = itemView.findViewById(R.id.aya_text_view);
        }
    }
}
