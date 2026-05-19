package com.neostride.app.feature.main.record;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.neostride.app.R;

import java.time.LocalDate;
import java.util.List;


//  기록 탭 월별 캘린더 어댑터
//  <p>
//  - 날짜별 러닝 거리 및 코칭 상태 점을 함께 표시한다.
//  - 선택된 날짜는 상태별 원형 배경으로 강조하며, 거리/점은 숨긴다.

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
    private final List<CalendarDayItem> days;
    private final OnDayClickListener listener;
    private int selectedPosition = -1;

    // 날짜 셀 클릭 콜백
    public interface OnDayClickListener {
        void onDayClick(CalendarDayItem day);
    }

    public CalendarAdapter(List<CalendarDayItem> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDayItem day = days.get(position);

        if (day == null) {
            holder.tvDay.setText("");
            holder.tvDistance.setVisibility(View.GONE);
            holder.viewSelected.setVisibility(View.GONE);
            holder.viewPlanDot.setVisibility(View.GONE);
            holder.itemView.setClickable(false);
            return;
        }

        int dayNum = day.getDate().getDayOfMonth();
        holder.tvDay.setText(String.valueOf(dayNum));
        holder.itemView.setClickable(true);

        boolean isToday = day.getDate().equals(LocalDate.now());
        boolean isSelected = position == selectedPosition;

        // ── 1. 선택 상태 처리 ──
        if (isSelected) {
            holder.viewSelected.setVisibility(View.VISIBLE);
            holder.tvDay.setTextColor(Color.parseColor("#000000")); // 선택 시 검정 글씨
            if (day.getCoachingStatus() != null) {
                switch (day.getCoachingStatus()) {
                    case "completed": holder.viewSelected.setBackgroundResource(R.drawable.bg_calendar_selected); break;
                    case "missed": holder.viewSelected.setBackgroundResource(R.drawable.bg_calendar_missed); break;
                    case "pending": holder.viewSelected.setBackgroundResource(R.drawable.bg_calendar_pending); break;
                }
            } else {
                holder.viewSelected.setBackgroundResource(R.drawable.bg_calendar_selected);
            }
        } else {
            holder.viewSelected.setVisibility(View.GONE);
            if (isToday) {
                holder.tvDay.setTextColor(Color.parseColor("#CCFF00"));
            } else {
                holder.tvDay.setTextColor(Color.parseColor("#FFFFFF"));
            }
        }

        holder.tvDay.setAlpha(day.isCurrentMonth() ? 1.0f : 0.3f);

        boolean hasCoaching = day.getCoachingStatus() != null;
        boolean hasDistance = day.hasDistance();

        // ── 2. 거리 + 코칭 dot 동시 표시 로직 ──
        if (isSelected) {
            // 선택 상태: 거리/dot 숨김
            holder.tvDistance.setVisibility(View.GONE);
            holder.viewPlanDot.setVisibility(View.GONE);
        } else if (hasDistance && hasCoaching) {
            // 둘 다 있을 때: dot(왼쪽) + 거리(오른쪽) 함께 표시
            holder.viewPlanDot.setVisibility(View.VISIBLE);
            switch (day.getCoachingStatus()) {
                case "completed": holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_selected); break;
                case "missed": holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_missed); break;
                case "pending": holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_pending); break;
            }
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText(day.getDistance());
            holder.tvDistance.setTextColor(Color.parseColor("#CCFF00"));
        } else if (hasDistance) {
            // 거리만 있을 때: 거리만 중앙에
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText(day.getDistance());
            holder.tvDistance.setTextColor(Color.parseColor("#CCFF00"));
            holder.viewPlanDot.setVisibility(View.GONE);
        } else if (hasCoaching) {
            // 코칭만 있을 때: dot만 표시, tv_distance는 INVISIBLE(constraint 유지용)
            holder.tvDistance.setVisibility(View.INVISIBLE);
            holder.tvDistance.setText("");
            holder.viewPlanDot.setVisibility(View.VISIBLE);
            switch (day.getCoachingStatus()) {
                case "completed": holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_selected); break;
                case "missed": holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_missed); break;
                case "pending": holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_pending); break;
            }
        } else {
            // 둘 다 없을 때
            holder.tvDistance.setVisibility(View.GONE);
            holder.viewPlanDot.setVisibility(View.GONE);
        }

        // ── 4. 클릭 이벤트 (갱신 호출) ──
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            // 이전 선택된 아이템을 다시 그려서 거리가 나타나게 함
            if (oldPos >= 0) notifyItemChanged(oldPos);
            // 새로 선택된 아이템을 다시 그려서 거리를 숨김
            notifyItemChanged(selectedPosition);

            listener.onDayClick(day);
        });
    }

    @Override
    public int getItemCount() { return days.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvDistance;
        View viewSelected, viewPlanDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            viewSelected = itemView.findViewById(R.id.view_selected_bg);
            viewPlanDot = itemView.findViewById(R.id.view_plan_dot);
        }
    }
}