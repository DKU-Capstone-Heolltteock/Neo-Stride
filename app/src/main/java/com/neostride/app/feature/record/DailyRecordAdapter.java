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
    private List<RunningRecordItem> records;
    private OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onRecordClick(RunningRecordItem record);
    }

    public DailyRecordAdapter(List<RunningRecordItem> records, OnRecordClickListener listener) {
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
        RunningRecordItem record = records.get(position);
        holder.tvDistance.setText(record.getDistance());
        holder.tvTime.setText(record.getTime());
        holder.tvPace.setText(record.getPace());
        holder.tvCalories.setText(record.getCalories());

        // AI Coaching 라벨 + accent bar 색상
        if (record.isAiCoaching()) {
            holder.tvAiLabel.setVisibility(android.view.View.VISIBLE);
            // GoalStorage에서 상태 확인하여 색상 결정
            String dateStr = record.getDate(); // created_at
            int barColor = 0xFFCCFF00; // 기본 형광
            try {
                String[] parts = dateStr.split("T")[0].split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int d = Integer.parseInt(parts[2]);
                String key = y + "-" + m + "-" + d;
                com.neostride.app.feature.coaching.GoalStorage.PlanData plan =
                        com.neostride.app.feature.coaching.GoalStorage.getPlan(holder.itemView.getContext(), key);
                if (plan != null) {
                    switch (plan.status) {
                        case "completed": barColor = 0xFFCCFF00; break; // 형광
                        case "missed": barColor = 0xFFFF3B30; break;    // 빨강
                        default: barColor = 0xFFFF9500; break;          // 주황
                    }
                }
            } catch (Exception e) { /* 파싱 실패시 형광 유지 */ }
            holder.accentBar.setBackgroundColor(barColor);
            holder.tvAiLabel.setTextColor(barColor);
        } else {
            holder.tvAiLabel.setVisibility(android.view.View.GONE);
            holder.accentBar.setBackgroundColor(0xFFCCFF00);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecordClick(record));
    }

    @Override
    public int getItemCount() { return records.size(); }

    public void updateData(List<RunningRecordItem> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDistance, tvTime, tvPace, tvCalories, tvAiLabel;
        android.view.View accentBar;
        ViewHolder(View view) {
            super(view);
            tvDistance = view.findViewById(R.id.tv_item_distance);
            tvTime = view.findViewById(R.id.tv_item_time);
            tvPace = view.findViewById(R.id.tv_item_pace);
            tvCalories = view.findViewById(R.id.tv_item_calories);
            tvAiLabel = view.findViewById(R.id.tv_ai_coaching_label);
            accentBar = view.findViewById(R.id.view_accent_bar);
        }
    }
}