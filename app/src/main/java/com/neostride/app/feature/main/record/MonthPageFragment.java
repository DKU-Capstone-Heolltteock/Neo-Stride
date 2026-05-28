package com.neostride.app.feature.main.record;

import android.graphics.Color;
import android.graphics.PorterDuff;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.community.common.util.DangerConfirmDialog;
import com.neostride.app.feature.main.MainActivity;
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
//  - ··· 버튼으로 다중 선택 모드 진입, 탭바 위치에 뜨는 액션 바로 선택 삭제를 지원한다.

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

    // ── 다중 선택 모드 상태 ──
    private boolean isSelectionMode = false;
    private OnBackPressedCallback selectionBackCallback;

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
        layoutDateHeader        = view.findViewById(R.id.layout_date_header);
        tvMoreOptions           = view.findViewById(R.id.tv_more_options);

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

        // 선택 수 변경 → activity 선택 바 카운트 업데이트
        dailyAdapter.setOnSelectionChangeListener(count -> notifySelectionCount(count));

        rvDailyRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDailyRecords.setAdapter(dailyAdapter);
        rvDailyRecords.setNestedScrollingEnabled(false);

        // 뒤로가기 인터셉트 — 선택 모드 중에만 활성화
        selectionBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                exitSelectionMode();
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), selectionBackCallback);

        setupPage();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAiGoalSection(LocalDate.now());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 프래그먼트 파괴 시 선택 모드가 켜져 있으면 탭바 복원
        if (isSelectionMode) hideActivitySelectionBar();
    }

    // ─── 외부 트리거용 ───
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

    // ─── 서버 데이터 로드; 선택된 날짜가 있으면 로드 완료 후 기록 목록도 재갱신 ───
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
                        if (selectedDay != null) onDaySelected(selectedDay);
                    });
                }
            }
            @Override
            public void onError(String message) { Log.e("NeoStride", message); }
        });
    }

    private void updateCalendarDistances(List<RunningRecordResponse> records) {
        if (currentDays == null || calendarAdapter == null) return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (CalendarDayItem dayItem : currentDays) {
            if (dayItem == null) continue;
            float dailyTotalDistance = 0f;
            boolean hasCoachingRecord = false;
            for (RunningRecordResponse res : records) {
                try {
                    LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter).toLocalDate();
                    if (resDate.equals(dayItem.getDate())) {
                        dailyTotalDistance += res.getDistance();
                        if (res.getPlanId() != null) hasCoachingRecord = true;
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            if (dailyTotalDistance > 0) {
                dayItem.setDistance(String.format(Locale.getDefault(), "%.2fkm", dailyTotalDistance));
            }
            if (hasCoachingRecord) dayItem.setCoachingStatus("completed");
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
                LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter).toLocalDate();
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

    // ─── 날짜 셀 선택 ───
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
                LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), formatter).toLocalDate();
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

        menuEdit.setVisibility(View.GONE);

        if (menuDelete instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) menuDelete;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView)
                    ((TextView) child).setText(isSelectionMode ? "선택 취소" : "선택하기");
                if (child instanceof ImageView)
                    ((ImageView) child).setImageResource(
                            isSelectionMode ? R.drawable.ic_x_circle : R.drawable.ic_check_circle);
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

        // 팝업 실제 너비를 measure 후 anchor 우측 끝에 맞춰 배치
        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidthPx = popupView.getMeasuredWidth();
        int xoff = anchor.getWidth() - popupWidthPx;
        popup.showAsDropDown(anchor, xoff, 0);
    }

    // ─── 다중 선택 모드 진입 ───
    private void enterSelectionMode() {
        isSelectionMode = true;
        selectionBackCallback.setEnabled(true);   // 뒤로가기 가로채기 ON
        dailyAdapter.enterSelectionMode();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showSelectionBar(() -> confirmDelete());
        }
    }

    // ─── 다중 선택 모드 종료 ───
    private void exitSelectionMode() {
        isSelectionMode = false;
        selectionBackCallback.setEnabled(false);  // 뒤로가기 가로채기 OFF
        dailyAdapter.exitSelectionMode();
        hideActivitySelectionBar();
    }

    private void hideActivitySelectionBar() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideSelectionBar();
        }
    }

    private void notifySelectionCount(int count) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateSelectionCount(count);
        }
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
                this::deleteSelectedRecords
        );
    }

    // ─── 선택된 기록 삭제 ───
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

    private void onAllDeleted() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            exitSelectionMode();
            fetchMonthDataFromServer();
        });
    }

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
}
