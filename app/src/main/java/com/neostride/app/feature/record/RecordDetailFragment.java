package com.neostride.app.feature.record;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.neostride.app.R;

public class RecordDetailFragment extends Fragment {

    // 페이스별 색상 (RunningFragment와 동일)
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    public static RecordDetailFragment newInstance(RunningRecord record) {
        RecordDetailFragment fragment = new RecordDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("record", record);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_detail, container, false);

        ImageView btnBack = view.findViewById(R.id.btn_detail_back);
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (getArguments() != null) {
            RunningRecord record = (RunningRecord) getArguments().getSerializable("record");
            if (record != null) {
                TextView tvTitle = view.findViewById(R.id.tv_detail_title);
                TextView tvDistance = view.findViewById(R.id.tv_detail_distance);
                TextView tvTime = view.findViewById(R.id.tv_detail_time);
                TextView tvPace = view.findViewById(R.id.tv_detail_pace);
                TextView tvCalories = view.findViewById(R.id.tv_detail_calories);

                tvTitle.setText("#1  " + record.getDate());
                tvDistance.setText(record.getDistance());
                tvTime.setText(record.getTime());
                tvPace.setText(record.getPace());
                tvCalories.setText(record.getCalories());

                // 페이스 차트 그리기
                LinearLayout chartLayout = view.findViewById(R.id.layout_pace_chart);
                drawPaceChart(chartLayout);
            }
        }

        return view;
    }

    private void drawPaceChart(LinearLayout container) {
        // 백엔드 연동 전이라 더미 데이터로 차트 시연
        // 실제로는 segment_paces 배열을 받아서 그려야 함
        float[] dummyPaces = {6.2f, 5.8f, 6.5f, 5.5f, 7.0f, 6.0f, 5.3f, 6.8f, 7.2f, 5.0f, 6.3f, 5.7f};

        float maxPace = 0;
        for (float p : dummyPaces) {
            if (p > maxPace) maxPace = p;
        }

        container.removeAllViews();

        for (int i = 0; i < dummyPaces.length; i++) {
            float pace = dummyPaces[i];

            // 바 높이 비율 (높을수록 느림 → 바가 높음)
            float heightRatio = pace / maxPace;
            int barHeight = (int) (heightRatio * 100);

            // 바 색상
            int color = getPaceColor(pace);

            // 바 뷰
            View bar = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dpToPx(barHeight), 1f);
            params.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            bar.setLayoutParams(params);
            bar.setBackgroundColor(color);

            // 바를 담는 컨테이너 (하단 정렬)
            LinearLayout barContainer = new LinearLayout(getContext());
            barContainer.setOrientation(LinearLayout.VERTICAL);
            barContainer.setGravity(Gravity.BOTTOM);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            barContainer.setLayoutParams(containerParams);
            barContainer.addView(bar);

            container.addView(barContainer);
        }
    }

    private int getPaceColor(float paceMinPerKm) {
        if (paceMinPerKm >= 7.0f) return COLOR_VERY_SLOW;
        if (paceMinPerKm >= 6.5f) return COLOR_SLOW;
        if (paceMinPerKm >= 5.8f) return COLOR_NORMAL;
        if (paceMinPerKm >= 5.2f) return COLOR_FAST;
        return COLOR_VERY_FAST;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}