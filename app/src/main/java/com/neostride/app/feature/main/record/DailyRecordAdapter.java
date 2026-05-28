package com.neostride.app.feature.main.record;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
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
//  - 다중 선택 모드에서는 각 카드 좌상단에 진한 회색 선택 원이 표시된다.

public class DailyRecordAdapter extends RecyclerView.Adapter<DailyRecordAdapter.ViewHolder> {
    private List<RunningRecordItem> records;
    private OnRecordClickListener listener;

    // 다중 선택 모드 상태
    private boolean isSelectionMode = false;
    private final Set<Long> selectedIds = new HashSet<>();
    private OnSelectionChangeListener selectionListener;

    // ── 선택 원 색상 (진한 회색) ──
    private static final int CIRCLE_COLOR = 0xFF666666;
    private static final int CIRCLE_STROKE_DP = 2;
    private static final int CIRCLE_INSET_DP  = 5;

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

        // ── 다중 선택 모드: 진한 회색 원 (빈원 / 채워진 원) ──
        if (isSelectionMode) {
            holder.ivSelectCircle.setVisibility(View.VISIBLE);
            boolean selected = selectedIds.contains(record.getId());
            holder.ivSelectCircle.setImageDrawable(
                    selected ? makeFilledCircle(holder.itemView) : makeEmptyCircle(holder.itemView));
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

    public void enterSelectionMode() {
        isSelectionMode = true;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() { return isSelectionMode; }
    public Set<Long> getSelectedIds() { return new HashSet<>(selectedIds); }
    public int getSelectedCount() { return selectedIds.size(); }

    // ── 빈 원 (선택 전): 진한 회색 테두리, 내부 투명 ──
    private static Drawable makeEmptyCircle(View v) {
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dp(v, CIRCLE_STROKE_DP), CIRCLE_COLOR);
        return ring;
    }

    // ── 채워진 원 (선택 후): 진한 회색 테두리 + 더 작은 흰색 원이 안에 채워짐 ──
    private static Drawable makeFilledCircle(View v) {
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dp(v, CIRCLE_STROKE_DP), CIRCLE_COLOR);

        GradientDrawable fill = new GradientDrawable();
        fill.setShape(GradientDrawable.OVAL);
        fill.setColor(Color.WHITE);

        InsetDrawable insetFill = new InsetDrawable(fill, dp(v, CIRCLE_INSET_DP));
        return new LayerDrawable(new Drawable[]{ring, insetFill});
    }

    private static int dp(View v, int value) {
        return Math.round(value * v.getResources().getDisplayMetrics().density);
    }

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
