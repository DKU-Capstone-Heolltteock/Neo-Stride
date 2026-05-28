package com.neostride.app.feature.main.record;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;
import com.neostride.app.feature.main.coaching.GoalStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//  하루 러닝 기록 목록 RecyclerView 어댑터
//  <p>
//  - AI 코칭 기록은 플랜 상태에 따른 색상 강조 바와 라벨을 표시한다.
//  - 일반 기록은 기본 형광색 강조 바만 표시한다.
//  - 다중 선택 모드에서는 각 카드 좌상단에 선택 원이 표시된다.

public class DailyRecordAdapter extends RecyclerView.Adapter<DailyRecordAdapter.ViewHolder> {
    private List<RunningRecordItem> records;
    private OnRecordClickListener listener;

    // 다중 선택 모드 상태
    private boolean isSelectionMode = false;
    private final Set<Long> selectedIds = new HashSet<>();
    private OnSelectionChangeListener selectionListener;

    // 기록 항목 클릭 콜백
    public interface OnRecordClickListener {
        void onRecordClick(RunningRecordItem record);
    }

    // 선택 수 변경 콜백
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }

    public DailyRecordAdapter(List<RunningRecordItem> records, OnRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener l) {
        this.selectionListener = l;
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

        // ── AI Coaching 색상 로직 ──
        if (record.isAiCoaching()) {
            holder.tvAiLabel.setVisibility(View.VISIBLE);
            int barColor = 0xFFFF9500;
            try {
                String dateStr = record.getDate();
                String[] parts = dateStr.split("T")[0].split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int d = Integer.parseInt(parts[2]);
                String key = y + "-" + m + "-" + d;
                GoalStorage.PlanData plan = GoalStorage.getPlan(holder.itemView.getContext(), key);
                if (plan != null) {
                    switch (plan.getEffectiveStatus(key)) {
                        case "completed": barColor = 0xFFCCFF00; break;
                        case "missed":    barColor = 0xFFFF3B30; break;
                        default:          barColor = 0xFFFF9500; break;
                    }
                }
            } catch (Exception e) { /* 안전하게 주황색 유지 */ }
            holder.accentBar.setBackgroundColor(barColor);
            holder.tvAiLabel.setTextColor(barColor);
        } else {
            holder.tvAiLabel.setVisibility(View.GONE);
            holder.accentBar.setBackgroundColor(0xFFCCFF00);
        }

        // ── 다중 선택 모드 원형 체크 표시 ──
        if (isSelectionMode) {
            holder.ivSelectCircle.setVisibility(View.VISIBLE);
            boolean selected = selectedIds.contains(record.getId());
            holder.ivSelectCircle.setImageResource(
                    selected ? R.drawable.ic_check_circle : R.drawable.ic_just_circle);
            holder.ivSelectCircle.setColorFilter(0xFFCCFF00, PorterDuff.Mode.SRC_IN);
        } else {
            holder.ivSelectCircle.setVisibility(View.GONE);
        }

        // ── 클릭 처리 ──
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                RunningRecordItem item = records.get(pos);
                long id = item.getId();
                if (selectedIds.contains(id)) selectedIds.remove(id);
                else selectedIds.add(id);
                notifyItemChanged(pos);
                if (selectionListener != null) selectionListener.onSelectionChanged(selectedIds.size());
            } else {
                listener.onRecordClick(record);
            }
        });
    }

    @Override
    public int getItemCount() { return records.size(); }

    public void updateData(List<RunningRecordItem> newRecords) {
        this.records = newRecords;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    // ── 다중 선택 모드 진입 ──
    public void enterSelectionMode() {
        isSelectionMode = true;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    // ── 다중 선택 모드 종료 ──
    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() { return isSelectionMode; }

    public Set<Long> getSelectedIds() { return new HashSet<>(selectedIds); }

    public int getSelectedCount() { return selectedIds.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDistance, tvTime, tvPace, tvCalories, tvAiLabel;
        View accentBar;
        ImageView ivSelectCircle;

        ViewHolder(View view) {
            super(view);
            tvDistance      = view.findViewById(R.id.tv_item_distance);
            tvTime          = view.findViewById(R.id.tv_item_time);
            tvPace          = view.findViewById(R.id.tv_item_pace);
            tvCalories      = view.findViewById(R.id.tv_item_calories);
            tvAiLabel       = view.findViewById(R.id.tv_ai_coaching_label);
            accentBar       = view.findViewById(R.id.view_accent_bar);
            ivSelectCircle  = view.findViewById(R.id.iv_select_circle);
        }
    }
}
