package com.neostride.app.feature.record;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import java.util.List;

public class DailyRecordAdapter extends RecyclerView.Adapter<DailyRecordAdapter.ViewHolder> {
    private List<RunningRecord> records;
    private OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onRecordClick(RunningRecord record);
    }

    public DailyRecordAdapter(List<RunningRecord> records, OnRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunningRecord record = records.get(position);
        holder.tvDistance.setText(record.getDistance());
        holder.tvTime.setText(record.getTime());
        holder.tvPace.setText(record.getPace());
        holder.tvCalories.setText(record.getCalories());

        holder.itemView.setOnClickListener(v -> listener.onRecordClick(record));
    }

    @Override
    public int getItemCount() { return records.size(); }

    public void updateData(List<RunningRecord> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDistance, tvTime, tvPace, tvCalories;
        ViewHolder(View view) {
            super(view);
            tvDistance = view.findViewById(R.id.tv_record_distance);
            tvTime = view.findViewById(R.id.tv_record_time);
            tvPace = view.findViewById(R.id.tv_record_pace);
            tvCalories = view.findViewById(R.id.tv_record_calories);
        }
    }
}