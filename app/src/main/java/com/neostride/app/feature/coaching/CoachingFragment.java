package com.neostride.app.feature.coaching;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.neostride.app.R;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CoachingFragment extends Fragment {

    private TextView tvWeekLabel, tvNoPlan, btnToday, tvHistoryEmpty;
    private ImageView ivHistoryArrow;
    private CardView btnToggleHistory;
    private SwipeDetector layoutWeekRow;
    private LinearLayout layoutPlanDetail;
    private TextView tvPlanTitle, tvPlanSummary, tvPlanStatus;
    private TextView tvSetPeriod, tvSetDays, tvSetDistance, tvSetPace;
    private TextView tvAiFeedback;
    private CardView btnAddGoal, btnDeleteGoal;
    private RecyclerView rvHistory;
    private boolean historyExpanded = false;

    private Calendar weekStart = Calendar.getInstance();
    private int selectedDay = -1, selectedMonth = -1, selectedYear = -1;

    static String dayKeyToKorean(String key) {
        switch (key) { case "sun": return "일"; case "mon": return "월"; case "tue": return "화"; case "wed": return "수"; case "thu": return "목"; case "fri": return "금"; case "sat": return "토"; default: return key; }
    }

    static String daysListToKorean(java.util.List<String> days) {
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
        layoutWeekRow = view.findViewById(R.id.layout_week_row);
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

        ImageView btnPrev = view.findViewById(R.id.btn_prev_week);
        ImageView btnNext = view.findViewById(R.id.btn_next_week);

        setToStartOfWeek(weekStart);
        updateWeekView();

        // 월 이동 — 다음/전 달의 1일이 속한 주로
        btnPrev.setOnClickListener(v -> {
            // 현재 주의 중간 날짜 기준으로 해당 월 구하기
            Calendar mid = (Calendar) weekStart.clone();
            mid.add(Calendar.DAY_OF_MONTH, 3); // 주의 중간(수요일쯤)
            int curMonth = mid.get(Calendar.MONTH);
            int curYear = mid.get(Calendar.YEAR);

            // 전 달 1일로
            Calendar target = Calendar.getInstance();
            target.clear();
            target.set(curYear, curMonth - 1, 1);
            setToStartOfWeek(target);
            weekStart = target;
            selectedDay = -1;
            updateWeekView();
        });

        btnNext.setOnClickListener(v -> {
            Calendar mid = (Calendar) weekStart.clone();
            mid.add(Calendar.DAY_OF_MONTH, 3);
            int curMonth = mid.get(Calendar.MONTH);
            int curYear = mid.get(Calendar.YEAR);

            // 다음 달 1일로
            Calendar target = Calendar.getInstance();
            target.clear();
            target.set(curYear, curMonth + 1, 1);
            setToStartOfWeek(target);
            weekStart = target;
            selectedDay = -1;
            updateWeekView();
        });

        btnToday.setOnClickListener(v -> {
            weekStart = Calendar.getInstance();
            setToStartOfWeek(weekStart);
            selectedDay = -1;
            updateWeekView();
        });

        // 스와이프 (주간 이동) — SwipeDetector 사용
        layoutWeekRow.setSwipeListener(new SwipeDetector.SwipeListener() {
            @Override
            public void onSwipeLeft() {
                weekStart.add(Calendar.WEEK_OF_YEAR, 1);
                selectedDay = -1;
                updateWeekView();
            }
            @Override
            public void onSwipeRight() {
                weekStart.add(Calendar.WEEK_OF_YEAR, -1);
                selectedDay = -1;
                updateWeekView();
            }
        });

        // 새 목표
        btnAddGoal.setOnClickListener(v -> {
            GoalSettingFragment goalFragment = new GoalSettingFragment();
            goalFragment.setOnGoalSavedListener(goalInput -> {
                GoalStorage.saveGoalToPlanDays(requireContext(), goalInput);
                updateWeekView();
            });
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, goalFragment)
                    .addToBackStack(null).commit();
        });

        // 삭제
        btnDeleteGoal.setOnClickListener(v -> {
            String dateKey = selectedYear + "-" + selectedMonth + "-" + selectedDay;
            GoalStorage.PlanData plan = GoalStorage.getPlan(requireContext(), dateKey);
            if (plan == null) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("목표 삭제")
                    .setMessage("이 목표를 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        GoalStorage.HistoryItem history = new GoalStorage.HistoryItem();
                        history.goalId = plan.goalId;
                        history.distanceKm = plan.distanceKm;
                        history.paceStr = plan.paceStr;
                        history.durationWeeks = plan.durationWeeks;
                        history.runningDaysStr = daysListToKorean(plan.runningDays);
                        history.result = "deleted";
                        history.timestamp = System.currentTimeMillis();
                        GoalStorage.addHistory(requireContext(), history);
                        GoalStorage.removeAllPlansForGoal(requireContext(), plan.goalId);
                        selectedDay = -1;
                        updateWeekView();
                        loadHistory();
                    })
                    .setNegativeButton("취소", null).show();
        });

        // 히스토리 토글
        btnToggleHistory.setOnClickListener(v -> {
            historyExpanded = !historyExpanded;
            // 화살표 회전 (접힘: 0도, 펼침: 180도)
            ivHistoryArrow.animate().rotation(historyExpanded ? 180f : 0f).setDuration(200).start();
            if (historyExpanded) {
                loadHistory();
            } else {
                rvHistory.setVisibility(View.GONE);
                tvHistoryEmpty.setVisibility(View.GONE);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWeekView();
        if (historyExpanded) loadHistory();
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
            HistorySwipeAdapter adapter = new HistorySwipeAdapter(items);
            rvHistory.setAdapter(adapter);

            // 스와이프로 삭제/복원
            ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }
                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int pos = viewHolder.getBindingAdapterPosition();
                    GoalStorage.HistoryItem item = items.get(pos);

                    Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
                    boolean hasActive = !allPlans.isEmpty();

                    String[] options;
                    if ("deleted".equals(item.result) && !hasActive) {
                        options = new String[]{"복원", "영구 삭제", "취소"};
                    } else {
                        options = new String[]{"영구 삭제", "취소"};
                    }

                    new AlertDialog.Builder(requireContext())
                            .setItems(options, (d, which) -> {
                                String sel = options[which];
                                if ("영구 삭제".equals(sel)) {
                                    GoalStorage.removeHistory(requireContext(), pos);
                                    loadHistory();
                                } else if ("복원".equals(sel)) {
                                    GoalStorage.GoalInputData gi = new GoalStorage.GoalInputData();
                                    gi.durationWeeks = item.durationWeeks;
                                    gi.distanceKm = item.distanceKm;
                                    gi.paceSecPerKm = parsePaceToSec(item.paceStr);
                                    gi.runningDays = parseDaysFromKorean(item.runningDaysStr);
                                    GoalStorage.saveGoalToPlanDays(requireContext(), gi);
                                    GoalStorage.removeHistory(requireContext(), pos);
                                    Toast.makeText(getContext(), "목표가 복원되었습니다", Toast.LENGTH_SHORT).show();
                                    updateWeekView();
                                    loadHistory();
                                } else {
                                    adapter.notifyItemChanged(pos);
                                }
                            })
                            .setOnCancelListener(di -> adapter.notifyItemChanged(pos))
                            .show();
                }
            });
            touchHelper.attachToRecyclerView(rvHistory);
        }
    }

    private void setToStartOfWeek(Calendar cal) {
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
    }

    private void updateWeekView() {
        Calendar today = Calendar.getInstance();
        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());

        // 새 목표 버튼 — 목표 있으면 숨김
        btnAddGoal.setVisibility(allPlans.isEmpty() ? View.VISIBLE : View.GONE);

        // 헤더
        Calendar midWeek = (Calendar) weekStart.clone();
        midWeek.add(Calendar.DAY_OF_MONTH, 3);
        tvWeekLabel.setText(String.format("%d년 %d월", midWeek.get(Calendar.YEAR), midWeek.get(Calendar.MONTH) + 1));

        // 오늘 버튼
        Calendar todayWeekStart = (Calendar) today.clone();
        setToStartOfWeek(todayWeekStart);
        boolean isCurrentWeek = weekStart.get(Calendar.YEAR) == todayWeekStart.get(Calendar.YEAR)
                && weekStart.get(Calendar.WEEK_OF_YEAR) == todayWeekStart.get(Calendar.WEEK_OF_YEAR);
        btnToday.setVisibility(isCurrentWeek ? View.GONE : View.VISIBLE);

        // 주간 날짜
        layoutWeekRow.removeAllViews();
        String[] dayLabels = {"일", "월", "화", "수", "목", "금", "토"};

        Calendar dayCal = (Calendar) weekStart.clone();
        for (int i = 0; i < 7; i++) {
            int dayNum = dayCal.get(Calendar.DAY_OF_MONTH);
            int dayMonth = dayCal.get(Calendar.MONTH) + 1;
            int dayYear = dayCal.get(Calendar.YEAR);
            String dateKey = dayYear + "-" + dayMonth + "-" + dayNum;
            GoalStorage.PlanData plan = allPlans.get(dateKey);

            boolean isToday = dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            boolean isSelected = dayNum == selectedDay && dayMonth == selectedMonth && dayYear == selectedYear;

            LinearLayout cell = new LinearLayout(getContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            cell.setPadding(0, 8, 0, 8);
            cell.setClickable(true); cell.setFocusable(true);

            TextView tvLabel = new TextView(getContext());
            tvLabel.setText(dayLabels[i]); tvLabel.setTextSize(12); tvLabel.setGravity(Gravity.CENTER);
            if (i == 0) tvLabel.setTextColor(Color.parseColor("#FF4444"));
            else if (i == 6) tvLabel.setTextColor(Color.parseColor("#4488FF"));
            else tvLabel.setTextColor(Color.parseColor("#888888"));
            cell.addView(tvLabel);

            TextView tvDay = new TextView(getContext());
            tvDay.setText(String.valueOf(dayNum)); tvDay.setTextSize(16); tvDay.setGravity(Gravity.CENTER);
            tvDay.setTextColor(Color.WHITE);

            float density = getResources().getDisplayMetrics().density;
            int circleSize = (int) (36 * density); // 36dp 원형

            if (isSelected) {
                tvDay.setTextColor(Color.BLACK);
                LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(circleSize, circleSize);
                dayParams.gravity = Gravity.CENTER;
                dayParams.topMargin = (int) (4 * density);
                tvDay.setLayoutParams(dayParams);

                // 상태별 색상으로 원형 배경
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                if (plan != null && "completed".equals(plan.status)) circle.setColor(Color.parseColor("#34C759"));
                else if (plan != null && "missed".equals(plan.status)) circle.setColor(Color.parseColor("#FF3B30"));
                else if (plan != null) circle.setColor(Color.parseColor("#FF9500"));
                else circle.setColor(Color.parseColor("#CCFF00"));
                tvDay.setBackground(circle);
            } else {
                LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(circleSize, circleSize);
                dayParams.gravity = Gravity.CENTER;
                dayParams.topMargin = (int) (4 * density);
                tvDay.setLayoutParams(dayParams);

                if (isToday) { tvDay.setTextColor(Color.parseColor("#CCFF00")); tvDay.setTextSize(18); }
            }
            cell.addView(tvDay);

            View dot = new View(getContext());
            int dotSize = (int) (6 * getResources().getDisplayMetrics().density); // 6dp
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(dotSize, dotSize);
            dp.topMargin = 4; dp.gravity = Gravity.CENTER; dot.setLayoutParams(dp);
            if (!isSelected && plan != null) {
                dot.setVisibility(View.VISIBLE);
                switch (plan.status) {
                    case "completed": dot.setBackgroundResource(R.drawable.bg_calendar_selected); break;
                    case "missed": dot.setBackgroundResource(R.drawable.bg_calendar_missed); break;
                    default: dot.setBackgroundResource(R.drawable.bg_calendar_pending); break;
                }
            } else dot.setVisibility(View.INVISIBLE);
            cell.addView(dot);

            int fDay = dayNum, fMonth = dayMonth, fYear = dayYear;
            cell.setOnClickListener(v -> { selectedDay = fDay; selectedMonth = fMonth; selectedYear = fYear; updateWeekView(); showPlanDetail(dateKey); });

            layoutWeekRow.addView(cell);
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (selectedDay != -1) showPlanDetail(selectedYear + "-" + selectedMonth + "-" + selectedDay);
        else { tvNoPlan.setVisibility(View.VISIBLE); layoutPlanDetail.setVisibility(View.GONE); }
    }

    private void showPlanDetail(String dateKey) {
        GoalStorage.PlanData plan = GoalStorage.getPlan(requireContext(), dateKey);
        if (plan != null) {
            tvNoPlan.setVisibility(View.GONE);
            layoutPlanDetail.setVisibility(View.VISIBLE);
            tvPlanTitle.setText(String.format("%02d/%02d Training Routine", selectedMonth, selectedDay));
            tvPlanSummary.setText(String.format("%.0fkm · 목표 페이스 %s/km", plan.distanceKm, plan.paceStr));

            View dot = layoutPlanDetail.findViewById(R.id.view_your_set_dot);
            switch (plan.status) {
                case "completed":
                    tvPlanStatus.setText("완료"); tvPlanStatus.setTextColor(0xFF34C759);
                    tvPlanTitle.setTextColor(0xFFCCFF00);
                    if (dot != null) dot.setBackgroundColor(0xFFCCFF00);
                    break;
                case "missed":
                    tvPlanStatus.setText("미완료"); tvPlanStatus.setTextColor(0xFFFF3B30);
                    tvPlanTitle.setTextColor(0xFFFF9500);
                    if (dot != null) dot.setBackgroundColor(0xFFFF9500);
                    break;
                default:
                    tvPlanStatus.setText("예정"); tvPlanStatus.setTextColor(0xFFFF9500);
                    tvPlanTitle.setTextColor(0xFFFF9500);
                    if (dot != null) dot.setBackgroundColor(0xFFFF9500);
                    break;
            }
            tvSetPeriod.setText("설정 기간 : " + plan.durationWeeks + "주");
            tvSetDays.setText("설정 러닝 데이 : " + daysListToKorean(plan.runningDays));
            tvSetDistance.setText("최종 목표 거리 : " + plan.distanceKm + "km");
            tvSetPace.setText("최종 목표 기록 : " + plan.paceStr + "/km");
            tvAiFeedback.setText("아직 피드백이 없습니다.\n오늘의 러닝을 완료하면 AI가 피드백을 제공합니다.");
        } else {
            tvNoPlan.setVisibility(View.VISIBLE);
            layoutPlanDetail.setVisibility(View.GONE);
        }
    }

    private int parsePaceToSec(String ps) { try { String[] p = ps.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); } catch (Exception e) { return 360; } }
    private java.util.List<String> parseDaysFromKorean(String ds) {
        java.util.List<String> r = new java.util.ArrayList<>();
        if (ds.contains("일")) r.add("sun"); if (ds.contains("월")) r.add("mon"); if (ds.contains("화")) r.add("tue");
        if (ds.contains("수")) r.add("wed"); if (ds.contains("목")) r.add("thu"); if (ds.contains("금")) r.add("fri"); if (ds.contains("토")) r.add("sat");
        if (r.isEmpty()) { r.add("mon"); r.add("wed"); r.add("fri"); } return r;
    }

    // 히스토리 어댑터 (스와이프 지원)
    class HistorySwipeAdapter extends RecyclerView.Adapter<HistorySwipeAdapter.VH> {
        List<GoalStorage.HistoryItem> items;
        HistorySwipeAdapter(List<GoalStorage.HistoryItem> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal_history, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            GoalStorage.HistoryItem item = items.get(pos);
            h.tvTitle.setText(String.format("%.0fkm · %s/km", item.distanceKm, item.paceStr));
            h.tvPeriod.setText(String.format("%d주 · %s", item.durationWeeks, item.runningDaysStr));
            if ("completed".equals(item.result)) {
                h.tvStatus.setText("완료"); h.tvStatus.setTextColor(Color.parseColor("#34C759")); h.viewDot.setBackgroundColor(Color.parseColor("#34C759"));
            } else {
                h.tvStatus.setText("삭제됨"); h.tvStatus.setTextColor(Color.parseColor("#FF3B30")); h.viewDot.setBackgroundColor(Color.parseColor("#FF3B30"));
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            View viewDot; TextView tvTitle, tvPeriod, tvStatus;
            VH(View v) { super(v); viewDot = v.findViewById(R.id.view_status_dot); tvTitle = v.findViewById(R.id.tv_history_title); tvPeriod = v.findViewById(R.id.tv_history_period); tvStatus = v.findViewById(R.id.tv_history_status); }
        }
    }
}