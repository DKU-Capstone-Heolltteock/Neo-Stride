package com.neostride.app.feature.coaching;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.neostride.app.R;
import com.neostride.app.feature.record.RecordDetailFragment;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CoachingFragment extends Fragment {

    private TextView tvWeekLabel, tvNoPlan, btnToday, tvHistoryEmpty;
    private ImageView ivHistoryArrow;
    private LinearLayout btnToggleHistory;
    private ViewPager2 vpWeek;
    private WeekPagerAdapter weekPagerAdapter;
    private LinearLayout layoutPlanDetail;
    private TextView tvPlanTitle, tvPlanSummary, tvPlanStatus;
    private TextView tvSetPeriod, tvSetDays, tvSetDistance, tvSetPace;
    private TextView tvAiFeedback;
    private CardView btnAddGoal;
    private LinearLayout btnDeleteGoal;
    private RecyclerView rvHistory;
    private boolean historyExpanded = false;

    private int selectedDay = -1, selectedMonth = -1, selectedYear = -1;
    private int forceHeaderMonth = -1, forceHeaderYear = -1;

    // WeekPageFragment에서 접근
    public int getSelectedDay() { return selectedDay; }
    public int getSelectedMonth() { return selectedMonth; }
    public int getSelectedYear() { return selectedYear; }

    static String dayKeyToKorean(String key) {
        switch (key) { case "sun": return "일"; case "mon": return "월"; case "tue": return "화"; case "wed": return "수"; case "thu": return "목"; case "fri": return "금"; case "sat": return "토"; default: return key; }
    }

    static String daysListToKorean(List<String> days) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) { if (i > 0) sb.append(", "); sb.append(dayKeyToKorean(days.get(i))); }
        return sb.toString();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_coaching, container, false);

        tvWeekLabel = view.findViewById(R.id.tv_week_label);
        tvNoPlan = view.findViewById(R.id.tv_no_plan);
        vpWeek = view.findViewById(R.id.vp_week);
        layoutPlanDetail = view.findViewById(R.id.layout_plan_detail);
        tvPlanTitle = view.findViewById(R.id.tv_plan_title);
        tvPlanSummary = view.findViewById(R.id.tv_plan_summary);
        tvPlanStatus = view.findViewById(R.id.tv_plan_status);
        tvSetPeriod = view.findViewById(R.id.tv_set_period);
        tvSetDays = view.findViewById(R.id.tv_set_days);
        tvSetDistance = view.findViewById(R.id.tv_set_distance);
        tvSetPace = view.findViewById(R.id.tv_set_pace);
        tvAiFeedback = view.findViewById(R.id.tv_ai_feedback);
        btnDeleteGoal = view.findViewById(R.id.btn_delete_goal);
        btnAddGoal = view.findViewById(R.id.btn_add_goal);
        btnToday = view.findViewById(R.id.btn_today);
        btnToggleHistory = view.findViewById(R.id.btn_toggle_history);
        rvHistory = view.findViewById(R.id.rv_history);
        tvHistoryEmpty = view.findViewById(R.id.tv_history_empty);
        ivHistoryArrow = view.findViewById(R.id.iv_history_arrow);

        // ViewPager2 설정
        weekPagerAdapter = new WeekPagerAdapter(this);
        vpWeek.setAdapter(weekPagerAdapter);
        vpWeek.setCurrentItem(weekPagerAdapter.getCurrentWeekPosition(), false);

        // 초기 헤더: 오늘 날짜 기준
        Calendar today = Calendar.getInstance();
        tvWeekLabel.setText(String.format("%d년 %d월", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1));

        // 페이지 변경 시 헤더 업데이트
        vpWeek.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (forceHeaderYear > 0) {
                    tvWeekLabel.setText(String.format("%d년 %d월", forceHeaderYear, forceHeaderMonth));
                } else if (selectedMonth > 0) {
                    tvWeekLabel.setText(String.format("%d년 %d월", selectedYear, selectedMonth));
                } else {
                    Calendar ws = weekPagerAdapter.getWeekStartForPosition(position);
                    ws.add(Calendar.DAY_OF_MONTH, 3);
                    tvWeekLabel.setText(String.format("%d년 %d월", ws.get(Calendar.YEAR), ws.get(Calendar.MONTH) + 1));
                }

                // 오늘 버튼 표시 여부
                boolean isCurrent = position == weekPagerAdapter.getCurrentWeekPosition();
                btnToday.setVisibility(isCurrent ? View.GONE : View.VISIBLE);

                // 스와이프로 이동 시 forceHeader 초기화
                forceHeaderMonth = -1;
                forceHeaderYear = -1;

                // 새 목표 버튼 상태 갱신
                Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
                btnAddGoal.setVisibility(allPlans.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        // 월 텍스트 클릭 → 년/월 선택 팝업
        tvWeekLabel.setOnClickListener(v -> showYearMonthPicker());

        // 오늘 버튼
        btnToday.setOnClickListener(v -> {
            selectedDay = -1;
            selectedMonth = -1;
            selectedYear = -1;
            forceHeaderMonth = -1;
            forceHeaderYear = -1;
            vpWeek.setCurrentItem(weekPagerAdapter.getCurrentWeekPosition(), true);
        });

        // 새 목표
        btnAddGoal.setOnClickListener(v -> {
            GoalSettingFragment goalFragment = new GoalSettingFragment();
            goalFragment.setOnGoalSavedListener(goalInput -> {
                GoalStorage.saveGoalToPlanDays(requireContext(), goalInput);
                refreshWeekPages();
            });
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, goalFragment)
                    .addToBackStack(null).commit();
        });

        //  삭제 버튼 리스너
        btnDeleteGoal.setOnClickListener(v -> {
            String dateKey = selectedYear + "-" + selectedMonth + "-" + selectedDay;
            GoalStorage.PlanData plan = GoalStorage.getPlan(requireContext(), dateKey);
            if (plan == null) return;

            // 투박한 AlertDialog는 지우고, 우리가 만든 커스텀 다이얼로그 호출!
            showDeleteConfirmDialog(plan.goalId, plan);
        });

        // 히스토리 토글
        btnToggleHistory.setOnClickListener(v -> {
            historyExpanded = !historyExpanded;
            ivHistoryArrow.animate().rotation(historyExpanded ? 180f : 0f).setDuration(200).start();
            if (historyExpanded) loadHistory();
            else { rvHistory.setVisibility(View.GONE); tvHistoryEmpty.setVisibility(View.GONE); }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchActiveGoalFromServer();
        if (historyExpanded) loadHistory();

        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
        btnAddGoal.setVisibility(allPlans.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // WeekPageFragment에서 호출
    public void onWeekDayClicked(int day, int month, int year, String dateKey) {
        selectedDay = day; selectedMonth = month; selectedYear = year;
        forceHeaderMonth = month; forceHeaderYear = year;
        tvWeekLabel.setText(String.format("%d년 %d월", year, month));
        showPlanDetail(dateKey);
    }

    private void refreshWeekPages() {
        // 현재 헤더 텍스트 보존
        String currentHeader = tvWeekLabel.getText().toString();
        int currentPos = vpWeek.getCurrentItem();

        weekPagerAdapter = new WeekPagerAdapter(this);
        vpWeek.setAdapter(weekPagerAdapter);
        vpWeek.setCurrentItem(currentPos, false);

        // 헤더 복원
        tvWeekLabel.setText(currentHeader);
    }

    private void showYearMonthPicker() {
        Calendar ws = weekPagerAdapter.getWeekStartForPosition(vpWeek.getCurrentItem());
        ws.add(Calendar.DAY_OF_MONTH, 3);
        int curYear = ws.get(Calendar.YEAR);
        int curMonth = ws.get(Calendar.MONTH);

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(16));

        LinearLayout pickerRow = new LinearLayout(requireContext());
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(Gravity.CENTER);
        pickerRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(2024); yearPicker.setMaxValue(2030);
        yearPicker.setValue(curYear); yearPicker.setWrapSelectorWheel(false);
        String[] yLabels = new String[7];
        for (int i = 0; i < 7; i++) yLabels[i] = (2024 + i) + "년";
        yearPicker.setDisplayedValues(yLabels);

        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(1); monthPicker.setMaxValue(12);
        monthPicker.setValue(curMonth + 1); monthPicker.setWrapSelectorWheel(true);
        String[] mLabels = new String[12];
        for (int i = 0; i < 12; i++) mLabels[i] = (i + 1) + "월";
        monthPicker.setDisplayedValues(mLabels);

        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(160), 1f);
        pp.setMargins(dp(8), 0, dp(8), 0);
        yearPicker.setLayoutParams(pp); monthPicker.setLayoutParams(pp);
        pickerRow.addView(yearPicker); pickerRow.addView(monthPicker);
        root.addView(pickerRow);

        View divider = new View(requireContext());
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(16); divider.setLayoutParams(divP);
        root.addView(divider);

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL); btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(12); btnRow.setLayoutParams(brP);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("취소"); btnCancel.setTextColor(Color.parseColor("#888888"));
        btnCancel.setTextSize(15); btnCancel.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnConfirm = new TextView(requireContext());
        btnConfirm.setText("완료"); btnConfirm.setTextColor(Color.parseColor("#888888"));
        btnConfirm.setTextSize(15); btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnConfirm.setOnClickListener(v2 -> {
            int selYear = yearPicker.getValue();
            int selMonth = monthPicker.getValue();

            selectedDay = 1; selectedMonth = selMonth; selectedYear = selYear;
            forceHeaderMonth = selMonth; forceHeaderYear = selYear;

            int pos = weekPagerAdapter.getPositionForDate(selYear, selMonth, 1);
            vpWeek.setCurrentItem(pos, true);
            tvWeekLabel.setText(String.format("%d년 %d월", selYear, selMonth));

            // 페이지 전환 후 1일 선택 표시
            vpWeek.post(() -> {
                Fragment page = getChildFragmentManager().findFragmentByTag("f" + pos);
                if (page instanceof WeekPageFragment) {
                    ((WeekPageFragment) page).selectDay(1, selMonth, selYear);
                }
            });

            dialog.dismiss();
        });

        btnRow.addView(btnCancel); btnRow.addView(btnConfirm);
        root.addView(btnRow);
        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dp(280); params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#1A1A1A")); bg.setCornerRadius(dp(16));
            root.setBackground(bg);
        }
        dialog.show();
    }

    private void showPlanDetail(String dateKey) {
        GoalStorage.PlanData plan = GoalStorage.getPlan(requireContext(), dateKey);
        View historyHeaderDivider = getView().findViewById(R.id.view_history_header_divider);
        if (plan != null) {
            tvNoPlan.setVisibility(View.GONE);
            layoutPlanDetail.setVisibility(View.VISIBLE);
            tvPlanTitle.setText(String.format("%02d/%02d Training Routine", selectedMonth, selectedDay));
            tvPlanSummary.setVisibility(View.GONE);

            // 1. 상태별 색상 테마 적용 (기존 로직 유지)
            int themeColor;
            String statusText;
            int statusBgColor;

            switch (plan.status) {
                case "completed":
                    themeColor = 0xFFCCFF00;
                    statusText = "완료";
                    statusBgColor = 0xFF1A2A05;
                    break;
                case "missed":
                    themeColor = 0xFFFF3B30;
                    statusText = "미완료";
                    statusBgColor = 0xFF2A1010;
                    break;
                default:
                    themeColor = 0xFFFF9500;
                    statusText = "예정";
                    statusBgColor = 0xFF2A1A05;
                    break;
            }

            if (historyHeaderDivider != null) {
                historyHeaderDivider.setBackgroundColor(themeColor);
            }

            tvPlanTitle.setTextColor(themeColor);
            tvPlanStatus.setText(statusText);
            tvPlanStatus.setTextColor(themeColor);
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setShape(GradientDrawable.RECTANGLE);
            statusBg.setCornerRadius(dp(13));
            statusBg.setStroke(dp(1), themeColor);
            statusBg.setColor(statusBgColor);
            tvPlanStatus.setBackground(statusBg);

            // 🌟 2. AI 미션 여부 판별 및 UI 가시성 조절 (추가된 핵심 로직)
            View layoutDailyMissionInfo = layoutPlanDetail.findViewById(R.id.layout_daily_mission_info);
            TextView tvNoMissionMessage = layoutPlanDetail.findViewById(R.id.tv_no_mission_message);

            ImageView ivDistStatus = layoutPlanDetail.findViewById(R.id.iv_distance_status);
            ImageView ivPaceStatus = layoutPlanDetail.findViewById(R.id.iv_pace_status);
            TextView tvDistStatus = layoutPlanDetail.findViewById(R.id.tv_distance_status);
            TextView tvPaceStatus = layoutPlanDetail.findViewById(R.id.tv_pace_status);

            if (plan.isAiMission) {
                // AI 미션인 경우: 수치 영역 노출, 메시지 숨김
                if (layoutDailyMissionInfo != null) layoutDailyMissionInfo.setVisibility(View.VISIBLE);
                if (tvNoMissionMessage != null) tvNoMissionMessage.setVisibility(View.GONE);

                tvDistStatus.setText("목표 거리 : " + plan.distanceKm + "km");
                tvPaceStatus.setText("목표 페이스 : " + plan.paceStr);

                // 상태 아이콘 설정 (기존 switch 문 로직 통합)
                if (plan.status.equals("completed")) {
                    ivDistStatus.setImageResource(R.drawable.ic_check_circle);
                    ivDistStatus.setColorFilter(0xFFCCFF00);
                    tvDistStatus.setTextColor(0xFFCCCCCC);
                    ivPaceStatus.setImageResource(R.drawable.ic_check_circle);
                    ivPaceStatus.setColorFilter(0xFFCCFF00);
                    tvPaceStatus.setTextColor(0xFFCCCCCC);
                } else if (plan.status.equals("missed")) {
                    ivDistStatus.setImageResource(R.drawable.ic_x_circle);
                    ivDistStatus.setColorFilter(0xFFFF3B30);
                    tvDistStatus.setTextColor(0xFFCCCCCC);
                    ivPaceStatus.setImageResource(R.drawable.ic_x_circle);
                    ivPaceStatus.setColorFilter(0xFFFF3B30);
                    tvPaceStatus.setTextColor(0xFFCCCCCC);
                } else {
                    ivDistStatus.setImageResource(R.drawable.ic_just_circle);
                    ivDistStatus.setColorFilter(0xFF888888);
                    tvDistStatus.setTextColor(0xFF888888);
                    ivPaceStatus.setImageResource(R.drawable.ic_just_circle);
                    ivPaceStatus.setColorFilter(0xFF888888);
                    tvPaceStatus.setTextColor(0xFF888888);
                }
            } else {
                // AI 미션이 아닌 경우: 수치 영역 숨김, 안내 메시지 노출
                if (layoutDailyMissionInfo != null) layoutDailyMissionInfo.setVisibility(View.GONE);
                if (tvNoMissionMessage != null) {
                    tvNoMissionMessage.setVisibility(View.VISIBLE);
                    tvNoMissionMessage.setText("AI 목표를 준비 중입니다.");
                }
            }

            // 3. 공통 UI 요소 처리 (기존 로직 보존)
            ImageView ivYourSetIcon = layoutPlanDetail.findViewById(R.id.iv_your_set_icon);
            ImageView ivAiFeedbackIcon = layoutPlanDetail.findViewById(R.id.iv_ai_feedback_icon);
            if (ivYourSetIcon != null) ivYourSetIcon.setColorFilter(themeColor);
            if (ivAiFeedbackIcon != null) ivAiFeedbackIcon.setColorFilter(themeColor);

            LinearLayout btnDel = layoutPlanDetail.findViewById(R.id.btn_delete_goal);
            if (btnDel != null) {
                GradientDrawable delBg = new GradientDrawable();
                delBg.setShape(GradientDrawable.RECTANGLE);
                delBg.setCornerRadius(dp(15));
                delBg.setColor(Color.TRANSPARENT);
                delBg.setStroke(dp(1), themeColor);
                btnDel.setBackground(delBg);
                TextView tvDelLabel = layoutPlanDetail.findViewById(R.id.tv_delete_label);
                if (tvDelLabel != null) tvDelLabel.setTextColor(themeColor);
            }

            LinearLayout layoutDoneBanner = layoutPlanDetail.findViewById(R.id.layout_done_banner);
            if (layoutDoneBanner != null) {
                layoutDoneBanner.setVisibility(plan.status.equals("completed") ? View.VISIBLE : View.GONE);
            }

            // 히스토리 및 카드 테두리 처리 (기존 로직 보존)
            ImageView ivHistoryIcon = getView().findViewById(R.id.iv_history_icon);
            if (ivHistoryIcon != null) ivHistoryIcon.setColorFilter(themeColor);
            if (ivHistoryArrow != null) ivHistoryArrow.setColorFilter(themeColor);

            LinearLayout cardMain = layoutPlanDetail.findViewById(R.id.card_plan_main);
            if (cardMain != null) {
                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setShape(GradientDrawable.RECTANGLE);
                cardBg.setCornerRadius(dp(16));
                cardBg.setColor(Color.parseColor("#1A1A1A"));
                cardBg.setStroke(dp(1), themeColor);
                cardMain.setBackground(cardBg);
            }

            View divider1 = layoutPlanDetail.findViewById(R.id.divider_1);
            View divider2 = layoutPlanDetail.findViewById(R.id.divider_2);
            int dividerColor = (themeColor & 0x00FFFFFF) | 0x33000000;
            if (divider1 != null) divider1.setBackgroundColor(dividerColor);
            if (divider2 != null) divider2.setBackgroundColor(dividerColor);

            LinearLayout historySection = getView().findViewById(R.id.layout_history_section);
            if (historySection != null) {
                GradientDrawable histBg = new GradientDrawable();
                histBg.setShape(GradientDrawable.RECTANGLE);
                histBg.setCornerRadius(dp(16));
                histBg.setColor(Color.parseColor("#1A1A1A"));
                histBg.setStroke(dp(1), themeColor);
                historySection.setBackground(histBg);
            }

            // Your Setting 및 AI 피드백
            tvSetPeriod.setText("설정 기간 : " + plan.durationWeeks + "주");
            tvSetDays.setText("설정 러닝 데이 : " + daysListToKorean(plan.runningDays));
            tvSetDistance.setText("최종 목표 거리 : " + plan.totalGoalDistanceKm + "km");
            tvSetPace.setText("최종 목표 기록 : " + plan.totalGoalPaceStr);

            if (plan.aiFeedbackComment != null && !plan.aiFeedbackComment.isEmpty()) {
                tvAiFeedback.setText(plan.aiFeedbackComment);
                tvAiFeedback.setTextColor(0xFFCCCCCC);
            } else if (plan.description != null && !plan.description.isEmpty()) {
                tvAiFeedback.setText(plan.description);
                tvAiFeedback.setTextColor(0xFF888888);
            } else {
                tvAiFeedback.setText("아직 피드백이 없습니다.\n오늘의 러닝을 완료하면 AI가 피드백을 제공합니다.");
                tvAiFeedback.setTextColor(0xFF888888);
            }

            // 상세기록 버튼 클릭 리스너 (기존 로직 보존)
            TextView btnViewDetail = layoutPlanDetail.findViewById(R.id.btn_view_detail);
            if (btnViewDetail != null) {
                btnViewDetail.setOnClickListener(v -> {
                    String dateStr = String.format("%d-%02d-%02d", selectedYear, selectedMonth, selectedDay);
                    com.neostride.app.feature.running.repository.RunningRepository repo = new com.neostride.app.feature.running.repository.RunningRepository();
                    repo.fetchUserRecords(com.neostride.app.common.network.TokenManager.getUserId(requireContext()), new com.neostride.app.feature.running.repository.RunningRepository.RecordCallback() {
                        @Override
                        public void onSuccess(java.util.List<com.neostride.app.feature.running.model.RunningRecordResponse> records) {
                            if (!isAdded()) return;
                            for (com.neostride.app.feature.running.model.RunningRecordResponse r : records) {
                                if (r.getCreatedAt() != null && r.getCreatedAt().startsWith(dateStr)) {
                                    requireActivity().runOnUiThread(() -> {
                                        RecordDetailFragment detailFragment = new RecordDetailFragment();
                                        Bundle args = new Bundle();
                                        args.putSerializable("record_data", r);
                                        detailFragment.setArguments(args);
                                        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, detailFragment).addToBackStack(null).commit();
                                    });
                                    break;
                                }
                            }
                        }
                        @Override public void onError(String message) { requireActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()); }
                    });
                });
            }
        } else {
            // 목표 없는 날 처리 (기존 로직 보존)
            tvNoPlan.setVisibility(View.VISIBLE);
            layoutPlanDetail.setVisibility(View.GONE);
            int defaultColor = 0xFFCCFF00;
            if (historyHeaderDivider != null) {
                historyHeaderDivider.setBackgroundColor(defaultColor);
            }
            GradientDrawable noPlanBg = new GradientDrawable();
            noPlanBg.setShape(GradientDrawable.RECTANGLE);
            noPlanBg.setCornerRadius(dp(16));
            noPlanBg.setColor(Color.parseColor("#1A1A1A"));
            noPlanBg.setStroke(dp(1), defaultColor);
            tvNoPlan.setBackground(noPlanBg);
            LinearLayout historySection = getView().findViewById(R.id.layout_history_section);
            if (historySection != null) {
                GradientDrawable histBg = new GradientDrawable();
                histBg.setShape(GradientDrawable.RECTANGLE);
                histBg.setCornerRadius(dp(16));
                histBg.setColor(Color.parseColor("#1A1A1A"));
                histBg.setStroke(dp(1), defaultColor);
                historySection.setBackground(histBg);
            }
            ImageView ivHistoryIcon = getView().findViewById(R.id.iv_history_icon);
            if (ivHistoryIcon != null) ivHistoryIcon.setColorFilter(defaultColor);
            if (ivHistoryArrow != null) ivHistoryArrow.setColorFilter(defaultColor);
        }
    }

    private void fetchActiveGoalFromServer() {
        int userId = com.neostride.app.common.network.TokenManager.getUserId(requireContext());
        com.neostride.app.feature.coaching.repository.CoachingRepository repo = new com.neostride.app.feature.coaching.repository.CoachingRepository();

        repo.getActiveGoal(userId, new com.neostride.app.feature.coaching.repository.CoachingRepository.OnResultListener<com.neostride.app.feature.coaching.model.GoalResponse>() {
            @Override
            public void onSuccess(com.neostride.app.feature.coaching.model.GoalResponse data) {
                if (!isAdded()) return;
                if (data.hasActiveGoal() && data.getPlanDays() != null) {
                    GoalStorage.clearAllPlans(requireContext());
                    com.neostride.app.feature.coaching.model.GoalResponse.GoalInfo goal = data.getGoal();
                    String goalId = "goal_server_" + data.getGoalId();

                    for (com.neostride.app.feature.coaching.model.PlanDayResponse planDay : data.getPlanDays()) {
                        // 1. 서버에서 받은 날짜 문자열("2026-05-02")을 분리하여 저장용 키(key)를 생성합니다.
                        String[] dateParts = planDay.getPlanDate().split("-");
                        int year = Integer.parseInt(dateParts[0]);
                        int month = Integer.parseInt(dateParts[1]);
                        int day = Integer.parseInt(dateParts[2]);
                        String key = year + "-" + month + "-" + day;

                        // 2. 해당 날짜에 저장할 새로운 데이터 객체(PlanData)를 생성합니다.
                        GoalStorage.PlanData plan = new GoalStorage.PlanData();
                        plan.goalId = goalId; // 서버에서 부여한 고유 목표 ID를 연결합니다.

                        // --- [A. 오늘의 미션 데이터] ---
                        plan.distanceKm = planDay.getDayDistanceKm(); // 오늘 뛰어야 할 거리 (예: 3.2km)를 넣습니다.
                        plan.paceSecPerKm = (int) (planDay.getDayPaceMinPerKm() * 60); // 오늘 페이스를 초 단위로 변환해 저장합니다.
                        plan.paceStr = planDay.getFormattedPace(); // 오늘 페이스 문자열(예: "6:00/km")을 저장합니다.
                        plan.isAiMission = true; // 서버 데이터는 AI 코칭 미션이므로 true로 설정하여 UI 수치를 노출합니다.

                        // --- [B. 최종 목표 데이터 (Your Setting 카드 고정용)] ---
                        // 이 부분이 추가되어야 '0.0km'와 'null'이 사라지고 선생님이 설정한 10km가 뜹니다!
                        if (goal != null) {
                            // 서버에서 받은 전체 목표 거리(예: 10.0km)를 모든 날짜 데이터에 똑같이 복사합니다.
                            plan.totalGoalDistanceKm = (float) goal.getGoalDistanceKm();

                            // 서버의 분 단위 소수점 페이스(예: 5.5)를 "5:30/km" 형태의 예쁜 문자열로 변환하여 저장합니다.
                            double goalPaceMin = goal.getGoalPaceMinPerKm();
                            int minutes = (int) goalPaceMin;
                            int seconds = (int) ((goalPaceMin - minutes) * 60);
                            plan.totalGoalPaceStr = String.format(java.util.Locale.KOREA, "%d:%02d/km", minutes, seconds);
                        }

                        // 3. 기타 상태 및 AI 피드백 정보를 채웁니다.
                        plan.durationWeeks = goal != null ? goal.getDurationWeeks() : 4; // 설정 주차 정보
                        plan.runningDays = goal != null ? goal.getRunningDays() : new java.util.ArrayList<>(); // 설정 요일 정보
                        plan.status = planDay.getStatus(); // 완료(completed), 예정(pending) 등의 상태
                        plan.description = planDay.getDescription(); // 훈련 설명 (예: "토요일 첫 코칭")
                        plan.aiFeedbackComment = planDay.getAiFeedbackComment(); // AI가 남긴 코멘트

                        // 4. 완성된 데이터를 로컬 저장소(SharedPreferences)에 날짜별로 안전하게 저장합니다.
                        GoalStorage.savePlan(requireContext(), key, plan);
                    }
                }
                requireActivity().runOnUiThread(() -> refreshWeekPages());
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("CoachingFragment", "서버 동기화 실패: " + message);
            }
        });
    }

    private void loadHistory() {
        List<GoalStorage.HistoryItem> items = GoalStorage.getHistory(requireContext());
        if (items.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        } else {
            tvHistoryEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
            rvHistory.setNestedScrollingEnabled(false);
            HistorySwipeAdapter adapter = new HistorySwipeAdapter(items);
            rvHistory.setAdapter(adapter);
        }
    }

    private int parsePaceToSec(String ps) { try { String[] p = ps.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); } catch (Exception e) { return 360; } }
    private java.util.List<String> parseDaysFromKorean(String ds) {
        java.util.List<String> r = new java.util.ArrayList<>();
        if (ds.contains("일")) r.add("sun"); if (ds.contains("월")) r.add("mon"); if (ds.contains("화")) r.add("tue");
        if (ds.contains("수")) r.add("wed"); if (ds.contains("목")) r.add("thu"); if (ds.contains("금")) r.add("fri"); if (ds.contains("토")) r.add("sat");
        if (r.isEmpty()) { r.add("mon"); r.add("wed"); r.add("fri"); } return r;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    // 히스토리 어댑터
    class HistorySwipeAdapter extends RecyclerView.Adapter<HistorySwipeAdapter.VH> {
        List<GoalStorage.HistoryItem> items;
        HistorySwipeAdapter(List<GoalStorage.HistoryItem> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal_history, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            GoalStorage.HistoryItem item = items.get(pos);
            h.tvTitle.setText(String.format("%.0fkm · %s", item.distanceKm, item.paceStr));
            h.tvPeriod.setText(String.format("%d주 · %s", item.durationWeeks, item.runningDaysStr));

            boolean isCompleted = "completed".equals(item.result);
            int statusColor = isCompleted ? 0xFFCCFF00 : 0xFFFF3B30;

            // dot
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(statusColor);
            h.viewDot.setBackground(dotBg);

            if (h.viewDivider != null) {
                if (pos == items.size() - 1) {
                    // 리스트의 마지막 항목이면 구분선을 숨깁니다.
                    h.viewDivider.setVisibility(View.GONE);
                    // 마지막 항목: 선을 숨기고 아래쪽에 여백(16dp)을 추가합니다.
                    h.itemView.setPadding(0, 0, 0, dp(8));
                } else {
                    // 마지막이 아니면 보이게 하고, 점 색상(statusColor)과 일치시킵니다.
                    h.viewDivider.setVisibility(View.VISIBLE);
                    h.viewDivider.setBackgroundColor(statusColor);
                    h.viewDivider.setAlpha(0.4f); // 가독성을 위해 투명도를 살짝 줍니다.
                    // 해당 항목을 다른곳에서 쓸것을 대비해 아래 여백 초기화 작업
                    h.itemView.setPadding(0, 0, 0, 0);
                }
            }

            // 복원 버튼 (삭제된 것만 + 활성 목표 없을 때)
            boolean hasActive = !GoalStorage.getAllPlans(h.itemView.getContext()).isEmpty();
            boolean canRestore = !isCompleted && !hasActive;
            h.btnRestore.setVisibility(canRestore ? View.VISIBLE : View.GONE);

            // 복원 버튼 스타일 (형광 테두리)
            if (canRestore) {
                h.btnRestore.setTextColor(0xFFCCFF00);
                GradientDrawable restoreBg = new GradientDrawable();
                restoreBg.setCornerRadius(dp(14)); restoreBg.setColor(Color.TRANSPARENT);
                restoreBg.setStroke(dp(1), 0xFFCCFF00);
                h.btnRestore.setBackground(restoreBg);
            }

            // 삭제 버튼 스타일 (빨강 테두리)
            h.btnDelete.setTextColor(0xFFFF3B30);
            GradientDrawable delBg = new GradientDrawable();
            delBg.setCornerRadius(dp(14)); delBg.setColor(Color.TRANSPARENT);
            delBg.setStroke(dp(1), 0xFFFF3B30);
            h.btnDelete.setBackground(delBg);

            // 삭제 클릭 → 팝업
            h.btnDelete.setOnClickListener(v -> showHistoryDialog(
                    "목표 영구 삭제",
                    "이 목표 기록을 영구적으로 삭제합니다.\n삭제된 기록은 복구할 수 없습니다.",
                    "삭제",
                    0xFFFF3B30,
                    () -> { GoalStorage.removeHistory(requireContext(), pos); loadHistory(); }
            ));

            // 복원 클릭 → 팝업
            h.btnRestore.setOnClickListener(v -> showHistoryDialog(
                    "목표 복원",
                    "이 목표를 다시 활성화합니다.\n이전 설정 그대로 새 플랜이 생성됩니다.",
                    "복원",
                    0xFFCCFF00,
                    () -> {
                        GoalStorage.GoalInputData gi = new GoalStorage.GoalInputData();
                        gi.durationWeeks = item.durationWeeks;
                        gi.distanceKm = item.distanceKm;
                        gi.paceSecPerKm = parsePaceToSec(item.paceStr);
                        gi.runningDays = parseDaysFromKorean(item.runningDaysStr);
                        GoalStorage.saveGoalToPlanDays(requireContext(), gi);
                        GoalStorage.removeHistory(requireContext(), pos);
                        refreshWeekPages(); loadHistory();
                    }
            ));
        }

        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            View viewDot, viewDivider; TextView tvTitle, tvPeriod, btnRestore, btnDelete;
            VH(View v) { super(v);viewDot = v.findViewById(R.id.view_status_dot); viewDivider = v.findViewById(R.id.view_divider); tvTitle = v.findViewById(R.id.tv_history_title); tvPeriod = v.findViewById(R.id.tv_history_period); btnRestore = v.findViewById(R.id.btn_history_restore); btnDelete = v.findViewById(R.id.btn_history_delete); }
        }
    }

    private void showDeleteConfirmDialog(String goalId, GoalStorage.PlanData plan) {
        final android.content.Context context = requireContext();
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 1. 레이아웃 생성 및 레드 보더 배경 적용
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(R.drawable.bg_popup_red_border);

        // 2. 제목 설정
        TextView tvTitle = new TextView(context);
        tvTitle.setText("목표 삭제");
        tvTitle.setTextColor(0xFFFF3B30);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        // 3. 안내 문구 (취소 버튼과 같은 회색)
        TextView tvMsg = new TextView(context);
        tvMsg.setText("이 목표와 관련된 모든 플랜이 삭제됩니다.\n정말 삭제하시겠습니까?");
        tvMsg.setTextColor(0xFF888888);
        tvMsg.setTextSize(15);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(16);
        tvMsg.setLayoutParams(msgP);
        root.addView(tvMsg);

        // 🌟 4. [추가] 구분선 (Divider) 생성
        View divider = new View(context);
        divider.setBackgroundColor(0xFF333333); // 어두운 회색 선
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(20); // 문구와의 간격
        divider.setLayoutParams(divP);
        root.addView(divider);

        // 5. 버튼 영역
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(16); // 구분선과의 간격
        btnRow.setLayoutParams(brP);

        // 취소 버튼 (회색 글자)
        TextView btnCancel = new TextView(context);
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        // 삭제 버튼 (빨간 배경 + 검정 글자)
        TextView btnConfirm = new TextView(context);
        btnConfirm.setText("삭제");
        btnConfirm.setTextColor(Color.BLACK); // 🌟 요청하신 검정색 글꼴
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));

        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setCornerRadius(dp(20));
        confirmBg.setColor(0xFFFF3B30);
        btnConfirm.setBackground(confirmBg);

        LinearLayout.LayoutParams confirmP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        confirmP.setMarginStart(dp(8));
        btnConfirm.setLayoutParams(confirmP);

        btnConfirm.setOnClickListener(v -> {
            // 1. 데이터 처리: 히스토리에 '삭제됨' 상태로 기록
            GoalStorage.HistoryItem history = new GoalStorage.HistoryItem();
            history.goalId = plan.goalId;
            history.distanceKm = plan.distanceKm;
            history.paceStr = plan.paceStr;
            history.durationWeeks = plan.durationWeeks;
            history.runningDaysStr = daysListToKorean(plan.runningDays);
            history.result = "deleted"; //
            history.timestamp = System.currentTimeMillis();
            GoalStorage.addHistory(context, history);

            // 2. 현재 활성화된 모든 플랜 삭제
            GoalStorage.removeAllPlansForGoal(context, goalId);

            // 🌟 3. [핵심 수정] UI 테마 즉시 리셋
            selectedDay = -1; // 선택된 날짜 초기화

            // null을 전달하여 테두리(Border)와 아이콘 색상을
            // 즉시 기본 형광 초록색(#CCFF00)으로 되돌립니다.
            // showPlanDetail 메서드 하단의 'else' 블록이 실행되면서 색상이 갱신됩니다.
            showPlanDetail(null);

            refreshWeekPages(); // 주간 달력 갱신
            if (historyExpanded) loadHistory(); // 하단 과거 목록 리사이클러뷰 갱신

            dialog.dismiss();
            Toast.makeText(context, "목표가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
        });

        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dp(300);
            window.setAttributes(params);
        }
        dialog.show();
    }

    private void showHistoryDialog(String title, String message, String actionText, int actionColor, Runnable onConfirm) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));

        // 제목
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(actionColor);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        // 메시지
        TextView tvMsg = new TextView(requireContext());
        tvMsg.setText(message);
        tvMsg.setTextColor(0xFF999999);
        tvMsg.setTextSize(14);
        LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgP.topMargin = dp(12);
        tvMsg.setLayoutParams(msgP);
        tvMsg.setLineSpacing(dp(4), 1f);
        root.addView(tvMsg);

        // 구분선
        View divider = new View(requireContext());
        divider.setBackgroundColor(0xFF333333);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(20);
        divider.setLayoutParams(divP);
        root.addView(divider);

        // 버튼 행
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(16);
        btnRow.setLayoutParams(brP);

        // 취소
        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("취소");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setTextSize(15);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        // 확인
        TextView btnConfirm = new TextView(requireContext());
        btnConfirm.setText(actionText);
        btnConfirm.setTextColor(Color.BLACK);
        btnConfirm.setTextSize(15);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setCornerRadius(dp(20));
        confirmBg.setColor(actionColor);
        btnConfirm.setBackground(confirmBg);
        LinearLayout.LayoutParams confirmP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        confirmP.setMarginStart(dp(8));
        btnConfirm.setLayoutParams(confirmP);
        btnConfirm.setOnClickListener(v -> { dialog.dismiss(); onConfirm.run(); });
        btnRow.addView(btnConfirm);

        root.addView(btnRow);

        // 다이얼로그 스타일
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A1A1A"));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), actionColor);
        root.setBackground(bg);

        dialog.setContentView(root);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.width = dp(300);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
        dialog.show();
    }
}