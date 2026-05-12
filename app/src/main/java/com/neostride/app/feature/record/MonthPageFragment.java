package com.neostride.app.feature.record;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.coaching.GoalStorage;
import com.neostride.app.feature.running.model.RunningRecordResponse;
import com.neostride.app.feature.running.repository.RunningRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthPageFragment extends Fragment {
    private static final String ARG_MONTH = "arg_month";

    private static final String ARG_TIP_MODE = "arg_tip_mode";
    private boolean isTipMode = false;

    private YearMonth displayMonth;
    private RecyclerView rvCalendar, rvDailyRecords;
    private TextView tvSelectedDate, tvNoRecord;
    private TextView tvStatDistance, tvStatPace, tvStatCalories;
    private ImageView ivCompareDistance, ivComparePace, ivCompareCalories;

    private DailyRecordAdapter dailyAdapter;
    private CalendarAdapter calendarAdapter;
    private List<CalendarDayItem> currentDays;
    private RunningRepository recordRepository;
    private List<RunningRecordResponse> allServerRecords = new ArrayList<>();
    // AI 목표 달성도 그래프 관련 변수 추가
    private LinearLayout layoutAiGoalAchievement, layoutAiGraphContent;
    private TextView tvGraphGoalInfo;
    private ImageView ivAiGraphArrow;
    private View btnToggleAiGraph;

    public static MonthPageFragment newInstance(
            YearMonth month,
            boolean isTipMode
    ) {

        MonthPageFragment fragment = new MonthPageFragment();

        Bundle args = new Bundle();
        args.putString(ARG_MONTH, month.toString());
        args.putBoolean(ARG_TIP_MODE, isTipMode);

        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_month_page, container, false);

        if (getArguments() != null) {

            displayMonth = YearMonth.parse(
                    getArguments().getString(ARG_MONTH)
            );

            isTipMode = getArguments().getBoolean(
                    ARG_TIP_MODE,
                    false
            );
        }

        recordRepository = new RunningRepository();

        rvCalendar = view.findViewById(R.id.rv_calendar);
        rvDailyRecords = view.findViewById(R.id.rv_daily_records);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvNoRecord = view.findViewById(R.id.tv_no_record);
        tvStatDistance = view.findViewById(R.id.tv_stat_distance);
        tvStatPace = view.findViewById(R.id.tv_stat_pace);
        tvStatCalories = view.findViewById(R.id.tv_stat_calories);
        ivCompareDistance = view.findViewById(R.id.iv_compare_distance);
        ivComparePace = view.findViewById(R.id.iv_compare_pace);
        ivCompareCalories = view.findViewById(R.id.iv_compare_calories);
        // AI 달성도 그래프 섹션 뷰 연결
        layoutAiGoalAchievement = view.findViewById(R.id.layout_ai_goal_achievement);
        layoutAiGraphContent = view.findViewById(R.id.layout_ai_graph_content);
        tvGraphGoalInfo = view.findViewById(R.id.tv_graph_goal_info);
        ivAiGraphArrow = view.findViewById(R.id.iv_ai_graph_arrow);
        btnToggleAiGraph = view.findViewById(R.id.btn_toggle_ai_graph);
        ivAiGraphArrow.setRotation(180f);

        // AI 달성도 그래프 펼치기/접기 리스너 설정
        btnToggleAiGraph.setOnClickListener(v -> {
            if (layoutAiGraphContent.getVisibility() == View.VISIBLE) {
                layoutAiGraphContent.setVisibility(View.GONE);
                ivAiGraphArrow.animate().rotation(180f).setDuration(200).start(); // 화살표 아래로
            } else {
                layoutAiGraphContent.setVisibility(View.VISIBLE);
                ivAiGraphArrow.animate().rotation(0f).setDuration(200).start();   // 화살표 위로
            }
        });

        dailyAdapter = new DailyRecordAdapter(new ArrayList<>(), item -> {
            RunningRecordResponse selectedFullData = null;
            for (RunningRecordResponse res : allServerRecords) {
                if (res.getCreatedAt().equals(item.getDate())) {
                    selectedFullData = res;
                    break;
                }
            }

            if (selectedFullData != null) {
                RecordDetailFragment detailFragment =
                        RecordDetailFragment.newInstance(
                                selectedFullData,
                                isTipMode
                        );
                int containerId = isTipMode
                        ? R.id.tip_record_container
                        : R.id.fragment_container;

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(containerId, detailFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Log.e("NeoStride", "상세 데이터를 찾을 수 없습니다.");
            }
        });

        rvDailyRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDailyRecords.setAdapter(dailyAdapter);
        rvDailyRecords.setNestedScrollingEnabled(false);

        setupPage();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🌟 화면에 다시 돌아올 때마다(코칭 탭을 갔다가 와도) 즉시 10.0km를 띄웁니다.
        updateAiGoalSection(LocalDate.now());
    }

    private void setupPage() {
        currentDays = generateDaysList(displayMonth);
        calendarAdapter = new CalendarAdapter(currentDays, day -> {
            if (day != null) onDaySelected(day);
        });
        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendar.setAdapter(calendarAdapter);

        fetchMonthDataFromServer();
    }

    private void fetchMonthDataFromServer() {
        int userId = TokenManager.getUserId(requireContext());
        recordRepository.fetchUserRecords(userId, new RunningRepository.RecordCallback() {
            @Override
            public void onSuccess(List<RunningRecordResponse> records) {
                allServerRecords = records;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateMonthlyStatistics(records);
                        updateCalendarDistances(records);

                        // 🌟 [추가] 차트 데이터를 필터링해서 그려주는 함수를 호출합니다!
                        updateLineChart(records);
                    });
                }
            }
            @Override public void onError(String message) { Log.e("NeoStride", message); }
        });
    }

    private void updateCalendarDistances(List<RunningRecordResponse> records) {
        if (currentDays == null || calendarAdapter == null) return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (CalendarDayItem dayItem : currentDays) {
            if (dayItem == null) continue;
            float dailyTotalDistance = 0f;
            for (RunningRecordResponse res : records) {
                try {
                    LocalDate resDate = LocalDate.parse(res.getCreatedAt(), formatter);
                    if (resDate.equals(dayItem.getDate())) dailyTotalDistance += res.getDistance();
                } catch (Exception e) { e.printStackTrace(); }
            }
            if (dailyTotalDistance > 0) {
                dayItem.setDistance(String.format(Locale.getDefault(), "%.2fkm", dailyTotalDistance));
            }
        }
        calendarAdapter.notifyDataSetChanged();
    }

    private void updateMonthlyStatistics(List<RunningRecordResponse> records) {
        float curDist = 0f, curCal = 0f; double curSec = 0;
        float prevDist = 0f, prevCal = 0f; double prevSec = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        YearMonth prevMonth = displayMonth.minusMonths(1);

        for (RunningRecordResponse res : records) {
            try {
                LocalDate resDate = LocalDate.parse(res.getCreatedAt(), formatter);
                YearMonth resMonth = YearMonth.from(resDate);
                if (resMonth.equals(displayMonth)) {
                    curDist += res.getDistance(); curCal += res.getCalories(); curSec += res.getTime();
                } else if (resMonth.equals(prevMonth)) {
                    prevDist += res.getDistance(); prevCal += res.getCalories(); prevSec += res.getTime();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        float curPace = (curDist > 0) ? (float)((curSec/60.0)/curDist) : 0;
        float prevPace = (prevDist > 0) ? (float)((prevSec/60.0)/prevDist) : 0;

        tvStatDistance.setText(String.format(Locale.getDefault(), "%.2f km", curDist));
        tvStatCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", curCal));
        if (curPace > 0) {
            int min = (int) curPace; int sec = (int) ((curPace - min) * 60);
            tvStatPace.setText(String.format(Locale.getDefault(), "%d:%02d/km", min, sec));
        } else { tvStatPace.setText("--:--/km"); }

        updateComparisonUI(ivCompareDistance, curDist, prevDist, true);
        updateComparisonUI(ivComparePace, curPace, prevPace, false);
        updateComparisonUI(ivCompareCalories, curCal, prevCal, true);
    }

    private void updateComparisonUI(ImageView view, float current, float previous, boolean higherIsBetter) {
        if (previous <= 0) { view.setVisibility(View.GONE); return; }
        view.setVisibility(View.VISIBLE);
        boolean improved = higherIsBetter ? (current > previous) : (current > 0 && current < previous);
        if (improved) {
            view.setImageResource(R.drawable.ic_double_arrow_up);
            view.setColorFilter(Color.parseColor("#CCFF00"), PorterDuff.Mode.SRC_IN);
        } else if (current == previous) { view.setVisibility(View.GONE); }
        else {
            view.setImageResource(R.drawable.ic_double_arrow_down);
            view.setColorFilter(Color.parseColor("#FF4444"), PorterDuff.Mode.SRC_IN);
        }
    }

    private void onDaySelected(CalendarDayItem day) {
        LocalDate date = day.getDate();
        String formattedDate = date.getYear() + "년 " + date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 " + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        tvSelectedDate.setText(formattedDate);
        tvSelectedDate.setVisibility(View.VISIBLE);
        updateAiGoalSection(date);
        List<RunningRecordItem> filteredItems = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // 수정됨: 날짜 기준 Coaching 체크 로직 삭제
        // 각 개별 기록(res)에 포함된 plan_id 유무로 AI Coaching 여부를 직접 판단합니다.

        for (RunningRecordResponse res : allServerRecords) {
            try {
                LocalDate resDate = LocalDate.parse(res.getCreatedAt(), formatter);
                if (resDate.equals(date)) {
                    RunningRecordItem item = convertToItem(res);

                    // 🔥 핵심 수정 사항: 개별 기록의 planId가 null이 아니면 AI Coaching으로 표시
                    if (res.getPlanId() != null) {
                        item.setAiCoaching(true);
                    } else {
                        item.setAiCoaching(false);
                    }

                    filteredItems.add(item);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (filteredItems.isEmpty()) {
            tvNoRecord.setVisibility(View.VISIBLE);
            dailyAdapter.updateData(new ArrayList<>());
        } else {
            tvNoRecord.setVisibility(View.GONE);
            dailyAdapter.updateData(filteredItems);
        }
    }

    private void updateAiGoalSection(LocalDate date) {
        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());

        if (allPlans != null && !allPlans.isEmpty()) {
            layoutAiGoalAchievement.setVisibility(View.VISIBLE);
            GoalStorage.PlanData baseGoal = allPlans.values().iterator().next();
            tvGraphGoalInfo.setText(String.format(Locale.KOREA, "• 설정한 목표 거리 : %.1fkm", baseGoal.totalGoalDistanceKm));
            setupPaceChart(baseGoal.totalGoalPaceStr);

            if (!allServerRecords.isEmpty()) {
                updateLineChart(allServerRecords);
            }
        } else {
            layoutAiGoalAchievement.setVisibility(View.GONE);
        }
    }

    private RunningRecordItem convertToItem(RunningRecordResponse res) {
        int totalSeconds = (int) res.getTime();
        String timeStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        return new RunningRecordItem(res.getCreatedAt(), String.format("%.2fkm", res.getDistance()), timeStr, String.format("%.2f/km", res.getPace()), (int)res.getCalories() + "kcal");
    }

    private List<CalendarDayItem> generateDaysList(YearMonth month) {
        List<CalendarDayItem> days = new ArrayList<>();
        LocalDate firstOfMonth = month.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < dayOfWeek; i++) days.add(null);
        Map<String, GoalStorage.PlanData> allPlans = (getContext() != null) ? GoalStorage.getAllPlans(getContext()) : null;
        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            LocalDate date = month.atDay(i);
            String coachingStatus = null;
            if (allPlans != null) {
                String key = date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
                GoalStorage.PlanData plan = allPlans.get(key);
                if (plan != null) coachingStatus = plan.status;
            }
            CalendarDayItem calDay = new CalendarDayItem(date, "", true);
            calDay.setCoachingStatus(coachingStatus);
            days.add(calDay);
        }
        return days;
    }

    // 파라미터를 double에서 String으로 변경합니다!
    private void setupPaceChart(String targetPaceStr) {
        if (getView() != null && targetPaceStr != null) {
            AiLineChartView chartView = getView().findViewById(R.id.ai_line_chart);
            if (chartView != null) {
                try {
                    // "5:30/km" -> "5:30" 추출
                    String timePart = targetPaceStr.split("/")[0];
                    String[] parts = timePart.split(":");
                    float min = Float.parseFloat(parts[0]);
                    float sec = Float.parseFloat(parts[1]);

                    // 분 단위 소수점으로 변환 (5 + 30/60 = 5.5)
                    float paceValue = min + (sec / 60f);

                    chartView.setTargetPace(paceValue); // 🌟 그래프에 목표치 전달
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateLineChart(List<RunningRecordResponse> records) {
        if (records == null || getView() == null) return;

        List<RunningRecordResponse> coachingRecords = new ArrayList<>();
        List<Float> targetList = new ArrayList<>();
        float finalGoalDist = 0f; // 🌟 최종 목표 거리를 담을 변수

        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());

        // 🌟 저장소에서 최종 목표 거리를 미리 하나 뽑아둡니다.
        if (allPlans != null && !allPlans.isEmpty()) {
            finalGoalDist = allPlans.values().iterator().next().totalGoalDistanceKm;
        }

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        for (RunningRecordResponse res : records) {
            if (res.getPlanId() != null) {
                coachingRecords.add(res);

                float dailyTarget = 0f;
                try {
                    java.time.LocalDate resDate = java.time.LocalDate.parse(res.getCreatedAt(), formatter);
                    String dateKey = resDate.getYear() + "-" + resDate.getMonthValue() + "-" + resDate.getDayOfMonth();
                    GoalStorage.PlanData plan = allPlans.get(dateKey);
                    if (plan != null) dailyTarget = plan.distanceKm;
                } catch (Exception e) { e.printStackTrace(); }
                targetList.add(dailyTarget);
            }
        }

        AiLineChartView chartView = getView().findViewById(R.id.ai_line_chart);
        if (chartView != null) {
            // 🌟 최종 목표 거리와 데이터를 모두 주입합니다.
            chartView.setFinalGoalDistance(finalGoalDist);
            chartView.setData(coachingRecords, targetList);
        }
    }
}