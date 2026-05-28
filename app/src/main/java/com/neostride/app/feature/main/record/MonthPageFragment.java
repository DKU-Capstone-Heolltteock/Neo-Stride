package com.neostride.app.feature.main.record;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.common.util.DangerConfirmDialog;
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
import java.util.Set;


//  월별 기록 페이지 Fragment
//  <p>
//  - ViewPager2의 한 페이지로, 특정 월의 캘린더 그리드·월간 통계·AI 달성도 그래프를 표시한다.
//  - 서버에서 러닝 기록을 가져와 날짜별 거리와 코칭 dot를 캘린더에 반영한다.
//  - 날짜 선택 시 해당 일의 상세 기록 목록을 아래 RecyclerView에 업데이트한다.
//  - AI 코칭 기록이 있으면 {@link AiLineChartView}에 실적 vs 목표 페이스를 그린다.
//  - ··· 버튼으로 다중 선택 모드 진입, 하단 액션 바로 선택 삭제를 지원한다.

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

    // ── 날짜 헤더 행 ──
    private LinearLayout layoutDateHeader;
    private TextView tvMoreOptions;

    // ── 다중 선택 하단 액션 바 ──
    private LinearLayout layoutSelectionBar;
    private TextView tvSelectionCount, tvDeleteSelected;
    private boolean isSelectionMode = false;

    // ── 어댑터 및 데이터 ──
    private DailyRecordAdapter dailyAdapter;
    private CalendarAdapter calendarAdapter;
    private List<CalendarDayItem> currentDays;
    private RunningRepository recordRepository;
    private List<RunningRecordResponse> allServerRecords = new ArrayList<>();

    // 현재 선택된 날짜 (삭제 후 리프레시용)
    private CalendarDayItem selectedDay;

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

        rvCalendar              = view.findViewById(R.id.rv_calendar);
        rvDailyRecords          = view.findViewById(R.id.rv_daily_records);
        tvSelectedDate          = view.findViewById(R.id.tv_selected_date);
        tvNoRecord              = view.findViewById(R.id.tv_no_record);
        tvStatDistance          = view.findViewById(R.id.tv_stat_distance);
        tvStatPace              = view.findViewById(R.id.tv_stat_pace);
        tvStatCalories          = view.findViewById(R.id.tv_stat_calories);
        ivCompareDistance       = view.findViewById(R.id.iv_compare_distance);
        ivComparePace           = view.findViewById(R.id.iv_compare_pace);
        ivCompareCalories       = view.findViewById(R.id.iv_compare_calories);
        layoutAiGoalAchievement = view.findViewById(R.id.layout_ai_goal_achievement);
        layoutAiGraphContent    = view.findViewById(R.id.layout_ai_graph_content);
        tvGraphGoalInfo         = view.findViewById(R.id.tv_graph_goal_info);
        ivAiGraphArrow          = view.findViewById(R.id.iv_ai_graph_arrow);
        btnToggleAiGraph        = view.findViewById(R.id.btn_toggle_ai_graph);

        // ── 날짜 헤더 행 ──
        layoutDateHeader = view.findViewById(R.id.layout_date_header);
        tvMoreOptions    = view.findViewById(R.id.tv_more_options);

        // ── 선택 하단 바 ──
        layoutSelectionBar = view.findViewById(R.id.layout_selection_bottom_bar);
        tvSelectionCount   = view.findViewById(R.id.tv_selection_count);
        tvDeleteSelected   = view.findViewById(R.id.tv_delete_selected);

        // 삭제 버튼 배경 (빨강 둥근 모서리)
        GradientDrawable deleteBg = new GradientDrawable();
        deleteBg.setColor(0xFFFF3B30);
        deleteBg.setCornerRadius(dp(20));
        tvDeleteSelected.setBackground(deleteBg);

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

        // ··· 버튼 클릭 → 팝업
        tvMoreOptions.setOnClickListener(v -> showMorePopup(tvMoreOptions));

        // 삭제하기 버튼
        tvDeleteSelected.setOnClickListener(v -> confirmDelete());

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

        // 선택 수 변경 콜백
        dailyAdapter.setOnSelectionChangeListener(count -> {
            tvSelectionCount.setText(count + "개 선택됨");
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
    //  선택된 날짜가 있으면 데이터 로드 후 해당 날의 기록 목록도 재갱신한다.
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
                        // 선택된 날짜가 있으면 기록 목록도 재갱신
                        if (selectedDay != null) onDaySelected(selectedDay);
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

    // ─── 날짜 셀 선택 시: 선택 모드 종료, 선택일 라벨 갱신, AI 목표 섹션 갱신, 해당 일 기록 목록 필터링 ───
    private void onDaySelected(CalendarDayItem day) {
        // 다른 날짜 선택 시 선택 모드 종료
        if (isSelectionMode && (selectedDay == null || !selectedDay.getDate().equals(day.getDate()))) {
            exitSelectionMode();
        }
        selectedDay = day;

        LocalDate date = day.getDate();
        String formattedDate = date.getYear() + "년 " + date.getMonthValue() + "월 "
                + date.getDayOfMonth() + "일 "
                + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        tvSelectedDate.setText(formattedDate);
        layoutDateHeader.setVisibility(View.VISIBLE);
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
            tvMoreOptions.setVisibility(View.GONE);
            dailyAdapter.updateData(new ArrayList<>());
        } else {
            tvNoRecord.setVisibility(View.GONE);
            tvMoreOptions.setVisibility(View.VISIBLE);
            dailyAdapter.updateData(filteredItems);
        }
    }

    // ─── ··· 팝업: 선택하기 / 선택 취소 ───
    private void showMorePopup(View anchor) {
        View popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_owner_more_options, null);
        View menuEdit   = popupView.findViewById(R.id.menu_edit);
        View menuDelete = popupView.findViewById(R.id.menu_delete);

        // 수정하기 항목 숨김
        menuEdit.setVisibility(View.GONE);

        // menu_delete 내 텍스트/아이콘 → "선택하기" 또는 "선택 취소"
        if (menuDelete instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) menuDelete;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText(isSelectionMode ? "선택 취소" : "선택하기");
                }
                if (child instanceof ImageView) {
                    ((ImageView) child).setImageResource(
                            isSelectionMode ? R.drawable.ic_x_circle : R.drawable.ic_check_circle);
                }
            }
        }

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        menuDelete.setOnClickListener(v -> {
            popup.dismiss();
            if (isSelectionMode) exitSelectionMode();
            else enterSelectionMode();
        });

        // popup 우측 끝이 anchor(···) 우측 끝에 맞닿도록 xoff 계산
        // layout_owner_more_options 고정 너비 140dp 기준
        int popupWidthPx = (int)(140 * getResources().getDisplayMetrics().density);
        int xoff = anchor.getWidth() - popupWidthPx;
        popup.showAsDropDown(anchor, xoff, 0);
    }

    // ─── 다중 선택 모드 진입 ───
    private void enterSelectionMode() {
        isSelectionMode = true;
        tvSelectionCount.setText("0개 선택됨");
        layoutSelectionBar.setVisibility(View.VISIBLE);
        dailyAdapter.enterSelectionMode();
    }

    // ─── 다중 선택 모드 종료 ───
    private void exitSelectionMode() {
        isSelectionMode = false;
        layoutSelectionBar.setVisibility(View.GONE);
        dailyAdapter.exitSelectionMode();
    }

    // ─── 선택된 기록 삭제 확인 다이얼로그 ───
    private void confirmDelete() {
        int count = dailyAdapter.getSelectedCount();
        if (count == 0) return;

        DangerConfirmDialog.show(
                requireContext(),
                "기록 삭제",
                count + "개의 러닝 기록을 삭제합니다.\n삭제한 기록은 복구할 수 없습니다.",
                "삭제",
                () -> deleteSelectedRecords()
        );
    }

    // ─── 선택된 기록 ID 목록을 순차적으로 서버에서 삭제 ───
    private void deleteSelectedRecords() {
        Set<Long> idsToDelete = dailyAdapter.getSelectedIds();
        if (idsToDelete.isEmpty()) return;

        final int[] remaining = {idsToDelete.size()};

        for (long recordId : idsToDelete) {
            recordRepository.deleteRecord(recordId, new RunningRepository.OnResultListener<Void>() {
                @Override
                public void onSuccess(Void data) {
                    remaining[0]--;
                    if (remaining[0] == 0) onAllDeleted();
                }
                @Override
                public void onError(String message) {
                    Log.e("NeoStride", "삭제 실패: " + message);
                    remaining[0]--;
                    if (remaining[0] == 0) onAllDeleted();
                }
            });
        }
    }

    // ─── 전체 삭제 완료 후 선택 모드 종료 및 데이터 재로드 ───
    private void onAllDeleted() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            exitSelectionMode();
            fetchMonthDataFromServer();
        });
    }

    // ─── GoalStorage에 저장된 플랜이 있으면 AI 달성도 섹션을 표시하고 차트 데이터 주입 ───
    private void updateAiGoalSection(LocalDate date) {
        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
        if (allPlans != null && !allPlans.isEmpty()) {
            layoutAiGoalAchievement.setVisibility(View.VISIBLE);
            GoalStorage.PlanData baseGoal = allPlans.values().iterator().next();
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
        int paceSeconds = res.getPace() < 60
                ? (int)(res.getPace() * 60)
                : (int) res.getPace();
        String paceStr = String.format(Locale.getDefault(), "%d:%02d/km", paceSeconds / 60, paceSeconds % 60);
        RunningRecordItem item = new RunningRecordItem(
                res.getCreatedAt(),
                String.format("%.2fkm", res.getDistance()),
                timeStr,
                paceStr,
                (int) res.getCalories() + "kcal"
        );
        item.setId(res.getRunRecordId());
        return item;
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
