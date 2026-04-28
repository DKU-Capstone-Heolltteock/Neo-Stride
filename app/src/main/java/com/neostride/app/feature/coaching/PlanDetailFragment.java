package com.neostride.app.feature.coaching;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.neostride.app.R;

import java.util.Map;

public class PlanDetailFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DETAIL = "detail";
    private static final String ARG_GOAL_ID = "goal_id";

    private OnGoalDeletedListener onGoalDeletedListener;

    public interface OnGoalDeletedListener {
        void onGoalDeleted();
    }

    public void setOnGoalDeletedListener(OnGoalDeletedListener listener) {
        this.onGoalDeletedListener = listener;
    }

    public static PlanDetailFragment newInstance(String title, String detail, String goalId) {
        PlanDetailFragment fragment = new PlanDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DETAIL, detail);
        args.putString(ARG_GOAL_ID, goalId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan_detail, container, false);

        ImageView btnBack = view.findViewById(R.id.btn_back);
        TextView tvTitle = view.findViewById(R.id.tv_plan_date_title);
        TextView tvSummary = view.findViewById(R.id.tv_plan_summary);
        TextView tvAiFeedback = view.findViewById(R.id.tv_ai_feedback);
        TextView btnDelete = view.findViewById(R.id.btn_delete_goal);

        TextView tvSetPeriod = view.findViewById(R.id.tv_set_period);
        TextView tvSetDays = view.findViewById(R.id.tv_set_days);
        TextView tvSetDistance = view.findViewById(R.id.tv_set_distance);
        TextView tvSetPace = view.findViewById(R.id.tv_set_pace);

        String goalId = getArguments() != null ? getArguments().getString(ARG_GOAL_ID, "") : "";

        if (getArguments() != null) {
            tvTitle.setText(getArguments().getString(ARG_TITLE, "Training Routine"));
            tvSummary.setText(getArguments().getString(ARG_DETAIL, ""));
        }

        // goalId로 플랜 정보 찾기
        GoalStorage.PlanData plan = findPlanByGoalId(goalId);
        if (plan != null) {
            tvSetPeriod.setText("설정 기간 : " + plan.durationWeeks + " weeks");
            tvSetDays.setText("설정 러닝 데이 : " + String.join(", ", plan.runningDays));
            tvSetDistance.setText("최종 목표 거리 : " + plan.distanceKm + "km");
            tvSetPace.setText("최종 목표 기록 : " + plan.paceStr + "/km");
        }

        // AI 피드백 (추후 백엔드 연동)
        tvAiFeedback.setText("아직 피드백이 없습니다.\n오늘의 러닝을 완료하면 AI가 피드백을 제공합니다.");

        // 뒤로가기
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // 목표 삭제
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("목표 삭제")
                    .setMessage("이 목표를 삭제하시겠습니까?\n해당 목표의 모든 일정이 삭제됩니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // 히스토리에 기록
                        if (plan != null) {
                            GoalStorage.HistoryItem history = new GoalStorage.HistoryItem();
                            history.goalId = goalId;
                            history.distanceKm = plan.distanceKm;
                            history.paceStr = plan.paceStr;
                            history.durationWeeks = plan.durationWeeks;
                            history.runningDaysStr = String.join(", ", plan.runningDays);
                            history.result = "deleted";
                            history.timestamp = System.currentTimeMillis();
                            GoalStorage.addHistory(requireContext(), history);
                        }

                        // 플랜 삭제
                        GoalStorage.removeAllPlansForGoal(requireContext(), goalId);

                        if (onGoalDeletedListener != null) {
                            onGoalDeletedListener.onGoalDeleted();
                        }

                        getParentFragmentManager().popBackStack();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        return view;
    }

    private GoalStorage.PlanData findPlanByGoalId(String goalId) {
        if (goalId == null || goalId.isEmpty()) return null;
        Map<String, GoalStorage.PlanData> allPlans = GoalStorage.getAllPlans(requireContext());
        for (GoalStorage.PlanData plan : allPlans.values()) {
            if (goalId.equals(plan.goalId)) {
                return plan;
            }
        }
        return null;
    }
}