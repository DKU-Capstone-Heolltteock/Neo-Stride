package com.neostride.app.feature.coaching;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.neostride.app.R;

import java.util.Calendar;
import java.util.Map;


//  주간 캘린더 한 페이지 Fragment
//  <p>
//  - 7일 셀을 프로그래밍 방식으로 생성하여 요일·날짜·플랜 상태 점을 표시한다.
//  - 셀 클릭 시 {@link CoachingFragment#onWeekDayClicked}를 호출하여 플랜 상세를 갱신한다.

public class WeekPageFragment extends Fragment {

    private static final String ARG_WEEK_START = "week_start";
    private LinearLayout root;
    private int currentSelectedIndex = -1; // 현재 페이지에서 선택된 셀 인덱스

    public static WeekPageFragment newInstance(long weekStartMillis) {
        WeekPageFragment f = new WeekPageFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_WEEK_START, weekStartMillis);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.setPadding(dp(12), 0, dp(12), dp(8));

        buildWeekCells();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 데이터 변경 후 돌아왔을 때 갱신
        if (root != null) {
            root.removeAllViews();
            buildWeekCells();
        }
    }

    // ─── 7일 셀 뷰를 동적으로 생성하여 root에 추가 ───
    private void buildWeekCells() {
        long weekStartMillis = getArguments() != null ? getArguments().getLong(ARG_WEEK_START) : System.currentTimeMillis();
        Calendar weekStart = Calendar.getInstance();
        weekStart.setTimeInMillis(weekStartMillis);

        Calendar today = Calendar.getInstance();
        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());

        // 부모 CoachingFragment에서 현재 선택된 날짜 가져오기
        int parentSelDay = -1, parentSelMonth = -1, parentSelYear = -1;
        Fragment parent = getParentFragment();
        if (parent instanceof CoachingFragment) {
            CoachingFragment cf = (CoachingFragment) parent;
            parentSelDay = cf.getSelectedDay();
            parentSelMonth = cf.getSelectedMonth();
            parentSelYear = cf.getSelectedYear();
        }

        String[] dayLabels = {"일", "월", "화", "수", "목", "금", "토"};
        float density = getResources().getDisplayMetrics().density;
        int circleSize = (int) (36 * density);

        Calendar dayCal = (Calendar) weekStart.clone();
        for (int i = 0; i < 7; i++) {
            int dayNum = dayCal.get(Calendar.DAY_OF_MONTH);
            int dayMonth = dayCal.get(Calendar.MONTH) + 1;
            int dayYear = dayCal.get(Calendar.YEAR);
            String dateKey = dayYear + "-" + dayMonth + "-" + dayNum;
            GoalStorage.PlanData plan = allPlans.get(dateKey);

            boolean isToday = dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            boolean isSelected = dayNum == parentSelDay && dayMonth == parentSelMonth && dayYear == parentSelYear;

            // 셀
            LinearLayout cell = new LinearLayout(requireContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            cell.setPadding(0, dp(8), 0, dp(8));
            cell.setClickable(true);
            cell.setFocusable(true);

            // 요일
            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(dayLabels[i]);
            tvLabel.setTextSize(12);
            tvLabel.setGravity(Gravity.CENTER);
            if (i == 0) tvLabel.setTextColor(Color.parseColor("#FF4444"));
            else if (i == 6) tvLabel.setTextColor(Color.parseColor("#4488FF"));
            else tvLabel.setTextColor(Color.parseColor("#888888"));
            cell.addView(tvLabel);

            // 날짜
            TextView tvDay = new TextView(requireContext());
            tvDay.setText(String.valueOf(dayNum));
            tvDay.setTextSize(14);
            tvDay.setGravity(Gravity.CENTER);
            tvDay.setTypeface(null, Typeface.BOLD);

            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(circleSize, circleSize);
            dayParams.gravity = Gravity.CENTER;
            dayParams.topMargin = (int) (4 * density);
            tvDay.setLayoutParams(dayParams);

            if (isSelected) {
                // 선택 상태: 상태별 색상 원형
                tvDay.setTextColor(Color.BLACK);
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                if (plan != null && "completed".equals(plan.status)) circle.setColor(Color.parseColor("#CCFF00"));
                else if (plan != null && "missed".equals(plan.status)) circle.setColor(Color.parseColor("#FF3B30"));
                else if (plan != null) circle.setColor(Color.parseColor("#FF9500"));
                else circle.setColor(Color.parseColor("#CCFF00"));
                tvDay.setBackground(circle);
            } else if (isToday) {
                tvDay.setTextColor(Color.parseColor("#CCFF00"));
            } else {
                tvDay.setTextColor(Color.WHITE);
            }
            cell.addView(tvDay);

            // dot — 선택된 날은 dot 숨김
            View dot = new View(requireContext());
            int dotSize = (int) (5 * density);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.topMargin = 0;
            dotParams.gravity = Gravity.CENTER;
            dot.setLayoutParams(dotParams);

            if (!isSelected && plan != null) {
                dot.setVisibility(View.VISIBLE);
                switch (plan.status) {
                    case "completed": dot.setBackgroundResource(R.drawable.bg_calendar_selected); break;
                    case "missed": dot.setBackgroundResource(R.drawable.bg_calendar_missed); break;
                    default: dot.setBackgroundResource(R.drawable.bg_calendar_pending); break;
                }
            } else {
                dot.setVisibility(View.INVISIBLE);
            }
            cell.addView(dot);

            // 클릭
            int fDay = dayNum, fMonth = dayMonth, fYear = dayYear;
            int cellIndex = i;
            cell.setOnClickListener(v -> {
                // 부모에 전달
                Fragment p = getParentFragment();
                if (p instanceof CoachingFragment) {
                    ((CoachingFragment) p).onWeekDayClicked(fDay, fMonth, fYear, dateKey);
                }

                // 이 페이지 내에서 선택 UI 갱신
                currentSelectedIndex = cellIndex;
                refreshSelectionUI(allPlans);
            });

            root.addView(cell);
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    // ─── 현재 선택된 셀(currentSelectedIndex)을 기준으로 모든 셀의 선택/점 UI를 갱신 ───
    private void refreshSelectionUI(Map<String, GoalStorage.PlanData> allPlans) {
        long weekStartMillis = getArguments() != null ? getArguments().getLong(ARG_WEEK_START) : System.currentTimeMillis();
        Calendar dayCal = Calendar.getInstance();
        dayCal.setTimeInMillis(weekStartMillis);
        Calendar today = Calendar.getInstance();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < root.getChildCount(); i++) {
            LinearLayout cell = (LinearLayout) root.getChildAt(i);
            TextView tvDay = (TextView) cell.getChildAt(1); // 날짜
            View dot = cell.getChildAt(2); // dot

            int dayNum = dayCal.get(Calendar.DAY_OF_MONTH);
            int dayMonth = dayCal.get(Calendar.MONTH) + 1;
            int dayYear = dayCal.get(Calendar.YEAR);
            String dateKey = dayYear + "-" + dayMonth + "-" + dayNum;
            GoalStorage.PlanData plan = allPlans.get(dateKey);

            boolean isToday = dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            boolean isSelected = (i == currentSelectedIndex);

            if (isSelected) {
                tvDay.setTextColor(Color.BLACK);
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                if (plan != null && "completed".equals(plan.status)) circle.setColor(Color.parseColor("#CCFF00"));
                else if (plan != null && "missed".equals(plan.status)) circle.setColor(Color.parseColor("#FF3B30"));
                else if (plan != null) circle.setColor(Color.parseColor("#FF9500"));
                else circle.setColor(Color.parseColor("#CCFF00"));
                tvDay.setBackground(circle);
                dot.setVisibility(View.INVISIBLE);
            } else {
                tvDay.setBackground(null);
                tvDay.setTextColor(isToday ? Color.parseColor("#CCFF00") : Color.WHITE);

                if (plan != null) {
                    dot.setVisibility(View.VISIBLE);
                    switch (plan.status) {
                        case "completed": dot.setBackgroundResource(R.drawable.bg_calendar_selected); break;
                        case "missed": dot.setBackgroundResource(R.drawable.bg_calendar_missed); break;
                        default: dot.setBackgroundResource(R.drawable.bg_calendar_pending); break;
                    }
                } else {
                    dot.setVisibility(View.INVISIBLE);
                }
            }

            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }


//      외부(CoachingFragment)에서 특정 날짜를 선택 상태로 강제 지정한다.
//
//      @param day   선택할 일(day)
//      @param month 선택할 월(month, 1-based)
//      @param year  선택할 연도

    public void selectDay(int day, int month, int year) {
        if (root == null || getArguments() == null) return;

        long weekStartMillis = getArguments().getLong(ARG_WEEK_START);
        Calendar dayCal = Calendar.getInstance();
        dayCal.setTimeInMillis(weekStartMillis);

        for (int i = 0; i < 7; i++) {
            if (dayCal.get(Calendar.DAY_OF_MONTH) == day
                    && (dayCal.get(Calendar.MONTH) + 1) == month
                    && dayCal.get(Calendar.YEAR) == year) {
                currentSelectedIndex = i;
                Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
                refreshSelectionUI(allPlans);

                // 플랜 상세도 표시
                String dateKey = year + "-" + month + "-" + day;
                Fragment p = getParentFragment();
                if (p instanceof CoachingFragment) {
                    ((CoachingFragment) p).onWeekDayClicked(day, month, year, dateKey);
                }
                return;
            }
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}