package com.neostride.app.feature.main.coaching;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//  코칭 탭 캘린더 어댑터 (월간 그리드용)
//  <p>
//  - 날짜별 플랜 상태(pending/completed/missed)에 따라 하단 점 색상을 표시한다.
//  - 선택된 날짜는 상태별 원형 배경으로 강조한다.

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private List<Integer> days = new ArrayList<>();
    private int selectedDay = -1;
    private int todayDay = -1;
    // 날짜 → 상태 매핑 ("pending"=주황, "completed"=초록, "missed"=빨강) */
    private Map<Integer, String> dayStatuses = new HashMap<>();
    private OnDayClickListener listener;

    // 날짜 클릭 콜백 인터페이스
    public interface OnDayClickListener {
        void onDayClick(int day);
    }

    public CalendarAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void setDays(List<Integer> days, int today, Map<Integer, String> dayStatuses) {
        this.days = days;
        this.todayDay = today;
        this.dayStatuses = dayStatuses;
        this.selectedDay = today;
        notifyDataSetChanged();
    }

    public void setSelectedDay(int day) {
        this.selectedDay = day;
        notifyDataSetChanged();
    }

    public int getSelectedDay() {
        return selectedDay;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int day = days.get(position);

        if (day == 0) {
            holder.tvDay.setText("");
            holder.viewSelectedBg.setVisibility(View.GONE);
            holder.viewPlanDot.setVisibility(View.GONE);
            holder.itemView.setClickable(false);
            return;
        }

        holder.tvDay.setText(String.valueOf(day));
        holder.itemView.setClickable(true);

        String status = dayStatuses.get(day);

        // 선택 상태
        if (day == selectedDay) {
            holder.viewSelectedBg.setVisibility(View.VISIBLE);
            // 선택 배경 색상은 상태에 따라
            if (status != null) {
                switch (status) {
                    case "completed":
                        holder.viewSelectedBg.setBackgroundResource(R.drawable.bg_calendar_selected); // 초록
                        break;
                    case "missed":
                        holder.viewSelectedBg.setBackgroundResource(R.drawable.bg_calendar_missed); // 빨강
                        break;
                    case "pending":
                    default:
                        holder.viewSelectedBg.setBackgroundResource(R.drawable.bg_calendar_pending); // 주황
                        break;
                }
            } else {
                holder.viewSelectedBg.setBackgroundResource(R.drawable.bg_calendar_selected); // 기본 초록
            }
            holder.tvDay.setTextColor(Color.parseColor("#000000"));
            holder.viewPlanDot.setVisibility(View.GONE);
        } else {
            holder.viewSelectedBg.setVisibility(View.GONE);

            // 오늘 날짜
            if (day == todayDay) {
                holder.tvDay.setTextColor(Color.parseColor("#CCFF00"));
            } else {
                holder.tvDay.setTextColor(Color.parseColor("#FFFFFF"));
            }

            // 플랜 있는 날짜 하단 점 (상태별 색상)
            if (status != null) {
                holder.viewPlanDot.setVisibility(View.VISIBLE);
                switch (status) {
                    case "completed":
                        holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_selected); // 초록
                        break;
                    case "missed":
                        holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_missed); // 빨강
                        break;
                    case "pending":
                    default:
                        holder.viewPlanDot.setBackgroundResource(R.drawable.bg_calendar_pending); // 주황
                        break;
                }
            } else {
                holder.viewPlanDot.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            selectedDay = day;
            notifyDataSetChanged();
            if (listener != null) listener.onDayClick(day);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        View viewSelectedBg;
        View viewPlanDot;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            viewSelectedBg = itemView.findViewById(R.id.view_selected_bg);
            viewPlanDot = itemView.findViewById(R.id.view_plan_dot);
        }
    }
}