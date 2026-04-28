package com.neostride.app.feature.record;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.feature.coaching.GoalStorage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordFragment extends Fragment {

    private RecyclerView rvCalendar, rvDailyRecords;
    private TextView tvMonthYear, tvSelectedDate, tvNoRecord;
    private TextView tvStatDistance, tvStatPace, tvStatCalories;
    private DailyRecordAdapter dailyAdapter;
    private CalendarAdapter calendarAdapter;
    private YearMonth displayMonth;
    private List<CalendarDay> currentDays;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        rvCalendar = view.findViewById(R.id.rv_calendar);
        rvDailyRecords = view.findViewById(R.id.rv_daily_records);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvNoRecord = view.findViewById(R.id.tv_no_record);
        tvStatDistance = view.findViewById(R.id.tv_stat_distance);
        tvStatPace = view.findViewById(R.id.tv_stat_pace);
        tvStatCalories = view.findViewById(R.id.tv_stat_calories);

        ImageView btnPrev = view.findViewById(R.id.btn_prev_month);
        ImageView btnNext = view.findViewById(R.id.btn_next_month);

        displayMonth = YearMonth.now();

        // 일일 기록 어댑터
        dailyAdapter = new DailyRecordAdapter(new ArrayList<>(), record -> {
            RecordDetailFragment detailFragment = RecordDetailFragment.newInstance(record);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvDailyRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDailyRecords.setAdapter(dailyAdapter);
        rvDailyRecords.setNestedScrollingEnabled(false);

        // 달력 어댑터
        setupCalendar();

        // 월 이동
        btnPrev.setOnClickListener(v -> {
            displayMonth = displayMonth.minusMonths(1);
            setupCalendar();
        });

        btnNext.setOnClickListener(v -> {
            displayMonth = displayMonth.plusMonths(1);
            setupCalendar();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCalendar();
    }

    private void setupCalendar() {
        tvMonthYear.setText(String.format("%d년 %d월", displayMonth.getYear(), displayMonth.getMonthValue()));

        currentDays = generateDaysList(displayMonth);
        calendarAdapter = new CalendarAdapter(currentDays, day -> {
            if (day != null) {
                onDaySelected(day);
            }
        });

        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendar.setAdapter(calendarAdapter);

        // 통계 초기화 (백엔드 없으니 "데이터 없음" 표시)
        tvStatDistance.setText("0.0 km");
        tvStatPace.setText("--:--/km");
        tvStatCalories.setText("0 kcal");

        // 선택 초기화
        tvSelectedDate.setVisibility(View.GONE);
        tvNoRecord.setVisibility(View.GONE);
        dailyAdapter.updateData(new ArrayList<>());
    }

    private void onDaySelected(CalendarDay day) {
        LocalDate date = day.getDate();
        String formattedDate = date.getMonthValue() + "월 " +
                date.getDayOfMonth() + "일 " +
                date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        tvSelectedDate.setText(formattedDate);
        tvSelectedDate.setVisibility(View.VISIBLE);

        // 실제 기록은 백엔드에서 가져와야 함
        // 지금은 기록 없음으로 표시
        tvNoRecord.setVisibility(View.VISIBLE);
        dailyAdapter.updateData(new ArrayList<>());
    }

    private List<CalendarDay> generateDaysList(YearMonth month) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate firstOfMonth = month.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;

        // 빈 공간
        for (int i = 0; i < dayOfWeek; i++) {
            days.add(null);
        }

        // 코칭 플랜 상태 가져오기
        Map<String, GoalStorage.PlanData> allPlans = null;
        if (getContext() != null) {
            allPlans = GoalStorage.getAllPlans(getContext());
        }

        LocalDate today = LocalDate.now();

        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            LocalDate date = month.atDay(i);

            // 실제 뛴 거리는 백엔드에서 가져와야 함 — 지금은 빈 문자열
            String distance = "";

            // 코칭 플랜 상태 확인
            String coachingStatus = null;
            if (allPlans != null) {
                String key = date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
                GoalStorage.PlanData plan = allPlans.get(key);
                if (plan != null) {
                    coachingStatus = plan.status; // pending / completed / missed
                }
            }

            CalendarDay calDay = new CalendarDay(date, distance, true);
            calDay.setCoachingStatus(coachingStatus);
            days.add(calDay);
        }
        return days;
    }
}