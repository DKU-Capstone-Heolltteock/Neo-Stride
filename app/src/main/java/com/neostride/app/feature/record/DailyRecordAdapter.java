package com.neostride.app.feature.record;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import com.neostride.app.feature.coaching.GoalStorage;

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

        // 🔥 AI Coaching 판별 로직 (isAiCoaching 값에 전적으로 의존)
        if (record.isAiCoaching()) {
            // 1. 라벨 보이기
            holder.tvAiLabel.setVisibility(View.VISIBLE);

            // 2. 코칭 상태에 따른 색상 결정 (기본 주황)
            int barColor = 0xFFFF9500;
            try {
                String dateStr = record.getDate(); // 예: "2026-05-03T11:00:00"
                String[] parts = dateStr.split("T")[0].split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int d = Integer.parseInt(parts[2]);
                String key = y + "-" + m + "-" + d;

                GoalStorage.PlanData plan = GoalStorage.getPlan(holder.itemView.getContext(), key);
                if (plan != null) {
                    switch (plan.status) {
                        case "completed":
                            barColor = 0xFFCCFF00; // 완료 시 형광 연두
                            break;
                        case "missed":
                            barColor = 0xFFFF3B30; // 실패 시 빨강
                            break;
                        default:
                            barColor = 0xFFFF9500; // 대기/진행 시 주황
                            break;
                    }
                }
            } catch (Exception e) {
                // 파싱 오류 시 안전하게 주황색 유지
            }

            holder.accentBar.setBackgroundColor(barColor);
            holder.tvAiLabel.setTextColor(barColor);
        } else {
            // 🔥 일반 러닝: 라벨 숨기고 바 색상을 기본 형광색으로 고정
            holder.tvAiLabel.setVisibility(View.GONE);
            holder.accentBar.setBackgroundColor(0xFFCCFF00); // 기본 형광 연두 (#CCFF00)
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
        View accentBar;

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