package com.neostride.app.feature.event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

import java.util.List;

public class PatchNoteAdapter extends RecyclerView.Adapter<PatchNoteAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PatchNote note);
    }

    private final List<PatchNote> list;
    private OnItemClickListener listener;

    public PatchNoteAdapter(List<PatchNote> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patch_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PatchNote note = list.get(position);
        holder.tvVersion.setText(note.version);
        holder.tvDate.setText(note.date);
        holder.tvSummary.setText(note.summary);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(note);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVersion, tvDate, tvSummary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVersion = itemView.findViewById(R.id.tv_version);
            tvDate    = itemView.findViewById(R.id.tv_date);
            tvSummary = itemView.findViewById(R.id.tv_summary);
        }
    }
}
