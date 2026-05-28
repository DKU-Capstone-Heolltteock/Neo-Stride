package com.neostride.app.feature.main.record;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.main.coaching.GoalStorage;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;
import com.neostride.app.feature.main.running.repository.RunningRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


//  월별 기록 페이지 Fragment
//  <p>
//  - ViewPager2의 한 페이지로, 특정 월의 캘린더 그리드·월간 통계·AI 달성도 그래프를 표시한다.
//  - 서버에서 러닝 기록을 가져와 날짜별 거리와 코칭 dot를 캘린더에 반영한다.
//  - 날짜 선택 시 해당 일의 상세 기록 목록을 아래 RecyclerView에 업데이트한다.
//  - AI 코칭 기록이 있으면 {@link AiLineChartView}에 실적 vs 목표 페이스를 그린다.

public class MonthPageFragment extends Fragment {

    private static final String ARG_MONTH = "arg_month";
    private static final String ARG_TIP_MODE = "arg_tip_mode";

    // ── UI 뷰 ──
    private YearMonth displayMonth;
    private boolean isTipMode = false;
    private RecyclerView rvCalendar, rvDailyRecords;
    private TextView tvSelectedDate, tvNoRecord;
    private TextView tvStatDistance, tvStatPace, tvStatCalories;
    private ImageView ivCompareDistance, ivComparePace, ivCompareCalories;

    // ── 어댑터 및 데이터 ──
    private DailyRecordAdapter dailyAdapter;
    private CalendarAdapter calendarAdapter;
    private List<CalendarDayItem> currentDays;
    private RunningRepository recordRepository;
    private List<RunningRecordResponse> allServerRecords = new ArrayList<>();

    // ── AI 달성도 그래프 섹션 뷰 ──
    private LinearLayout layoutAiGoalAchievement, layoutAiGraphContent;
    private TextView tvGraphGoalInfo;
    private ImageView ivAiGraphArrow;
    private View btnToggleAiGraph;

    public static MonthPageFragment newInstance(YearMonth month, boolean isTipMode) {
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
            displayMonth = YearMonth.parse(getArguments().getString(ARG_MONTH));
            isTipMode = getArguments().getBoolean(ARG_TIP_MODE, false);
        }

        recordRepository = new RunningRepository();

        rvCalendar          = view.findViewById(R.id.rv_calendar);
        rvDailyRecords      = view.findViewById(R.id.rv_daily_records);
        tvSelectedDate      = view.findViewById(R.id.tv_selected_date);
        tvNoRecord          = view.findViewById(R.id.tv_no_record);
        tvStatDistance      = view.findViewById(R.id.tv_stat_distance);
        tvStatPace          = view.findViewById(R.id.tv_stat_pace);
        tvStatCalories      = view.findViewById(R.id.tv_stat_calories);
        ivCompareDistance   = view.findViewById(R.id.iv_compare_distance);
        ivComparePace       = view.findViewById(R.id.iv_compare_pace);
        ivCompareCalories   = view.findViewById(R.id.iv_compare_calories);
        layoutAiGoalAchievement = view.findViewById(R.id.layout_ai_goal_achievement);
        layoutAiGraphContent    = view.findViewById(R.id.layout_ai_graph_content);
        tvGraphGoalInfo         = view.findViewById(R.id.tv_graph_goal_info);
        ivAiGraphArrow          = view.findViewById(R.id.iv_ai_graph_arrow);
        btnToggleAiGraph        = view.findViewById(R.id.btn_toggle_ai_graph);
        ivAiGraphArrow.setRotation(180f);

        // AI 달성도 그래프 펼치기/접기
        btnToggleAiGraph.setOnClickListener(v -> {
            if (layoutAiGraphContent.getVisibility() == View.VISIBLE) {
                layoutAiGraphContent.setVisibility(View.GONE);
                ivAiGraphArrow.animate().rotation(180f).setDuration(200).start();
            } else {
                layoutAiGraphContent.setVisibility(View.VISIBLE);
                ivAiGraphArrow.animate().rotation(0f).setDuration(200).start();
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
                RecordDetailFragment detailFragment = RecordDetailFragment.newInstance(selectedFullData, isTipMode);
                int containerId = isTipMode ? R.id.tip_record_container : R.id.fragment_container;
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
        updateAiGoalSection(LocalDate.now());
    }

    // ─── 외부 트리거용: 새 측정 기록이 추가됐을 수 있으니 서버 데이터를 다시 가져와 UI 갱신 ───
    //  (RecordFragment.onHiddenChanged에서 호출 — add/hide/show 패턴 하에서 onResume이 안 불리는 케이스 보완)
    public void refresh() {
        if (!isAdded() || getContext() == null) return;
        fetchMonthDataFromServer();
        updateAiGoalSection(LocalDate.now());
    }

    // ─── 캘린더 어댑터 초기화 후 서버 데이터 요청 ───
    private void setupPage() {
        currentDays = generateDaysList(displayMonth);
        calendarAdapter = new CalendarAdapter(currentDays, day -> {
            if (day != null) onDaySelected(day);
        });
        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendar.setAdapter(calendarAdapter);
        fetchMonthDataFromServer();
    }

    // ─── 서버에서 전체 러닝 기록을 가져와 UI(통계·캘린더·차트)를 갱신 ───
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
                        updateLineChart(records);
                    });
                }
            }
            @Override
            public void onError(String message) { Log.e("NeoStride", message); }
        });
    }

    // ─── 서버 기록을 순회해 날짜별 누적 거리와 코칭 dot 상태를 캘린더에 반영 ───
    private void updateCalendarDistances(List<RunningRecordResponse> records) {
        if (currentDays == null || calendarAdapter == null) return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (CalendarDayItem dayItem : currentDays) {
            if (dayItem == null) continue;
            float dailyTotalDistance = 0f;
            boolean hasCoachingRecord = false;
            for (RunningRecordResponse res : records) {
                try {
                    // UTC → KST 변환 후 날짜 비교
                    LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter)
                            .toLocalDate();
                    if (resDate.equals(dayItem.getDate())) {
                        dailyTotalDistance += res.getDistance();
                        if (res.getPlanId() != null) hasCoachingRecord = true;
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            if (dailyTotalDistance > 0) {
                dayItem.setDistance(String.format(Locale.getDefault(), "%.2fkm", dailyTotalDistance));
            }
            if (hasCoachingRecord) {
                dayItem.setCoachingStatus("completed");
            }
        }
        calendarAdapter.notifyDataSetChanged();
    }

    // ─── 이번 달·전달 누계를 집계해 총 거리·페이스·칼로리 통계와 전월 비교 화살표 표시 ───
    private void updateMonthlyStatistics(List<RunningRecordResponse> records) {
        float curDist = 0f, curCal = 0f; double curSec = 0;
        float prevDist = 0f, prevCal = 0f; double prevSec = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        YearMonth prevMonth = displayMonth.minusMonths(1);

        for (RunningRecordResponse res : records) {
            try {
                LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter)
                        .toLocalDate();
                YearMonth resMonth = YearMonth.from(resDate);
                if (resMonth.equals(displayMonth)) {
                    curDist += res.getDistance(); curCal += res.getCalories(); curSec += res.getTime();
                } else if (resMonth.equals(prevMonth)) {
                    prevDist += res.getDistance(); prevCal += res.getCalories(); prevSec += res.getTime();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        float curPace  = (curDist  > 0) ? (float)((curSec  / 60.0) / curDist)  : 0;
        float prevPace = (prevDist > 0) ? (float)((prevSec / 60.0) / prevDist) : 0;

        tvStatDistance.setText(String.format(Locale.getDefault(), "%.2f km", curDist));
        tvStatCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", curCal));
        if (curPace > 0) {
            int min = (int) curPace;
            int sec = (int) ((curPace - min) * 60);
            tvStatPace.setText(String.format(Locale.getDefault(), "%d:%02d/km", min, sec));
        } else {
            tvStatPace.setText("--:--/km");
        }

        updateComparisonUI(ivCompareDistance, curDist, prevDist, true);
        updateComparisonUI(ivComparePace, curPace, prevPace, false);
        updateComparisonUI(ivCompareCalories, curCal, prevCal, true);
    }

    // ─── 전월 대비 증감에 따라 화살표 아이콘과 색상(형광/빨강) 설정; 전월 데이터 없으면 숨김 ───
    private void updateComparisonUI(ImageView view, float current, float previous, boolean higherIsBetter) {
        if (previous <= 0) { view.setVisibility(View.GONE); return; }
        view.setVisibility(View.VISIBLE);
        boolean improved = higherIsBetter ? (current > previous) : (current > 0 && current < previous);
        if (improved) {
            view.setImageResource(R.drawable.ic_double_arrow_up);
            view.setColorFilter(Color.parseColor("#CCFF00"), PorterDuff.Mode.SRC_IN);
        } else if (current == previous) {
            view.setVisibility(View.GONE);
        } else {
            view.setImageResource(R.drawable.ic_double_arrow_down);
            view.setColorFilter(Color.parseColor("#FF4444"), PorterDuff.Mode.SRC_IN);
        }
    }

    // ─── 날짜 셀 선택 시: 선택일 라벨 갱신, AI 목표 섹션 갱신, 해당 일 기록 목록 필터링 ───
    private void onDaySelected(CalendarDayItem day) {
        LocalDate date = day.getDate();
        String formattedDate = date.getYear() + "년 " + date.getMonthValue() + "월 "
                + date.getDayOfMonth() + "일 "
                + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        tvSelectedDate.setText(formattedDate);
        tvSelectedDate.setVisibility(View.VISIBLE);
        updateAiGoalSection(date);

        List<RunningRecordItem> filteredItems = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (RunningRecordResponse res : allServerRecords) {
            try {
                LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter)
                        .toLocalDate();
                if (resDate.equals(date)) {
                    RunningRecordItem item = convertToItem(res);
                    item.setAiCoaching(res.getPlanId() != null);
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

    // ─── GoalStorage에 저장된 플랜이 있으면 AI 달성도 섹션을 표시하고 차트 데이터 주입 ───
    private void updateAiGoalSection(LocalDate date) {
        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
        if (allPlans != null && !allPlans.isEmpty()) {
            layoutAiGoalAchievement.setVisibility(View.VISIBLE);
            GoalStorage.PlanData baseGoal = allPlans.values().iterator().next();
            // 전체 코칭 기간의 최종 목표 거리 표시 (고정값)
            tvGraphGoalInfo.setText(String.format(Locale.KOREA, "• 설정한 목표 거리 : %.2fkm", baseGoal.totalGoalDistanceKm));
            setupPaceChart(baseGoal.totalGoalPaceStr);
            if (!allServerRecords.isEmpty()) updateLineChart(allServerRecords);
        } else {
            layoutAiGoalAchievement.setVisibility(View.GONE);
        }
    }

    // ─── RunningRecordResponse → RunningRecordItem 변환 (시간·페이스 포맷 처리 포함) ───
    private RunningRecordItem convertToItem(RunningRecordResponse res) {
        int totalSeconds = (int) res.getTime();
        String timeStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        // pace < 60이면 구버전(분 단위), >= 60이면 신버전(초 단위)
        int paceSeconds = res.getPace() < 60
                ? (int)(res.getPace() * 60)
                : (int) res.getPace();
        String paceStr = String.format(Locale.getDefault(), "%d:%02d/km", paceSeconds / 60, paceSeconds % 60);
        return new RunningRecordItem(
                res.getCreatedAt(),
                String.format("%.2fkm", res.getDistance()),
                timeStr,
                paceStr,
                (int) res.getCalories() + "kcal"
        );
    }

    // ─── 해당 월의 캘린더 셀 목록 생성 (앞 빈칸 null 포함, 코칭 상태 주입) ───
    private List<CalendarDayItem> generateDaysList(YearMonth month) {
        List<CalendarDayItem> days = new ArrayList<>();
        LocalDate firstOfMonth = month.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < dayOfWeek; i++) days.add(null);

        Map<String, GoalStorage.PlanData> allPlans =
                (getContext() != null) ? GoalStorage.getAllPlans(getContext()) : null;

        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            LocalDate date = month.atDay(i);
            String coachingStatus = null;
            if (allPlans != null) {
                String key = date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
                GoalStorage.PlanData plan = allPlans.get(key);
                // getEffectiveStatus: 지난 pending 날짜를 missed로 동적 계산
                if (plan != null) coachingStatus = plan.getEffectiveStatus(key);
            }
            CalendarDayItem calDay = new CalendarDayItem(date, "", true);
            calDay.setCoachingStatus(coachingStatus);
            days.add(calDay);
        }
        return days;
    }

    // ─── 목표 페이스 문자열("5:30/km")을 파싱해 AiLineChartView에 기준선으로 전달 ───
    private void setupPaceChart(String targetPaceStr) {
        if (getView() == null || targetPaceStr == null) return;
        AiLineChartView chartView = getView().findViewById(R.id.ai_line_chart);
        if (chartView == null) return;
        try {
            String timePart = targetPaceStr.split("/")[0];
            String[] parts = timePart.split(":");
            float paceValue = Float.parseFloat(parts[0]) + Float.parseFloat(parts[1]) / 60f;
            chartView.setTargetPace(paceValue);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─── AI 코칭 기록과 날짜별 목표 거리를 추출해 AiLineChartView에 주입 ───
    private void updateLineChart(List<RunningRecordResponse> records) {
        if (records == null || getView() == null) return;

        List<RunningRecordResponse> coachingRecords = new ArrayList<>();
        List<Float> targetList = new ArrayList<>();
        float finalGoalDist = 0f;

        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
        if (allPlans != null && !allPlans.isEmpty()) {
            finalGoalDist = allPlans.values().iterator().next().totalGoalDistanceKm;
        }

        // 날짜별로 그룹화 — 하루에 점 1개 (달성한 기록 우선, 없으면 마지막 기록)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        java.util.LinkedHashMap<String, RunningRecordResponse> dailyBestMap = new java.util.LinkedHashMap<>();
        java.util.HashMap<String, Float> dailyTargetMap = new java.util.HashMap<>();

        for (RunningRecordResponse res : records) {
            if (res.getPlanId() == null) continue;
            try {
                LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter).toLocalDate();
                if (!YearMonth.from(resDate).equals(displayMonth)) continue;
                String dateKey = resDate.getYear() + "-" + resDate.getMonthValue() + "-" + resDate.getDayOfMonth();

                GoalStorage.PlanData plan = allPlans != null ? allPlans.get(dateKey) : null;
                float dailyTarget = plan != null ? plan.distanceKm : 0f;
                int dailyPaceSec = plan != null ? plan.paceSecPerKm : 0;

                // 이미 달성한 기록이 있으면 덮어쓰지 않음, 없으면 최신 기록으로 업데이트
                boolean alreadyAchieved = false;
                if (dailyBestMap.containsKey(dateKey)) {
                    RunningRecordResponse prev = dailyBestMap.get(dateKey);
                    float prevPace = prev.getPace() < 60 ? prev.getPace() * 60 : prev.getPace();
                    boolean prevDist = dailyTarget > 0 && prev.getDistance() >= dailyTarget;
                    boolean prevPaceOk = dailyPaceSec > 0 && prevPace <= dailyPaceSec;
                    alreadyAchieved = prevDist && prevPaceOk;
                }
                if (!alreadyAchieved) {
                    dailyBestMap.put(dateKey, res);
                    dailyTargetMap.put(dateKey, dailyTarget);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        for (java.util.Map.Entry<String, RunningRecordResponse> entry : dailyBestMap.entrySet()) {
            coachingRecords.add(entry.getValue());
            targetList.add(dailyTargetMap.getOrDefault(entry.getKey(), 0f));
        }

        AiLineChartView chartView = getView().findViewById(R.id.ai_line_chart);
        if (chartView != null) {
            chartView.setFinalGoalDistance(finalGoalDist);
            chartView.setData(coachingRecords, targetList);
        }
    }
}