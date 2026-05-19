package com.neostride.app.feature.badge;

import android.graphics.Color;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.badge.api.BadgeService;
import com.neostride.app.feature.badge.model.BadgeDetailResponse;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.badge.repository.BadgeRepository;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;
import com.neostride.app.feature.main.running.repository.RunningRepository;

import java.util.List;


//  뱃지 화면 Activity
//  <p>
//  - 현재 사용자의 뱃지 등급·기록·달성일자 표시
//  - 등급 계산기: km + 페이스 입력 시 {@link BadgeTier} 기준으로 예상 등급 산출
//  - 등급 기준표 확장/축소 토글

public class BadgeActivity extends AppCompatActivity {

    // ── 상태 ──
    private boolean isExpanded = false;

    // ── UI 뷰 ──
    private TextView tvBadgeTierName, tvBadgeRecord, tvBadgeDate;
    private ImageView ivBadgeMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badge);

        tvBadgeTierName = findViewById(R.id.tv_badge_tier_name);
        ivBadgeMain = findViewById(R.id.iv_badge_main);
        tvBadgeRecord = findViewById(R.id.tv_badge_record);
        tvBadgeDate = findViewById(R.id.tv_badge_date);

        ConstraintLayout layoutRoot = findViewById(R.id.layout_root);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout btnTierTable = findViewById(R.id.btn_tier_table);
        LinearLayout layoutExpanded = findViewById(R.id.layout_expanded);
        ImageView ivArrow = findViewById(R.id.iv_arrow);

        EditText etInputKm = findViewById(R.id.et_input_km);
        EditText etInputPace = findViewById(R.id.et_input_pace);
        TextView tvCalcResult = findViewById(R.id.tv_calc_result);
        TextView btnCalcCheck = findViewById(R.id.btn_calc_check);

        // 등급 기준 확장 애니메이션
        btnTierTable.setOnClickListener(v -> {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(300);
            TransitionManager.beginDelayedTransition(layoutRoot, transition);

            if (!isExpanded) {
                layoutExpanded.setVisibility(View.VISIBLE);
                ivArrow.setRotation(180f);
            } else {
                layoutExpanded.setVisibility(View.GONE);
                ivArrow.setRotation(0f);
            }
            isExpanded = !isExpanded;
        });

        // [수정된 부분] 확인 버튼 클릭 시 Enum 로직 사용
        btnCalcCheck.setOnClickListener(v -> {
            String strKm = etInputKm.getText().toString().trim();
            String strPace = etInputPace.getText().toString().trim();

            if (strKm.isEmpty() || strPace.isEmpty()) {
                tvCalcResult.setText("등급 : -");
                tvCalcResult.setTextColor(Color.parseColor("#CCFF00"));
                return;
            }

            try {
                double km = Double.parseDouble(strKm);
                if (km < 1.0 || km > 45.0) {
                    tvCalcResult.setText(km < 1.0 ? "거리 : 최소 1km" : "거리 : 최대 45km");
                    tvCalcResult.setTextColor(Color.parseColor("#FF0000"));
                    return;
                }

                if (!strPace.contains(":") || strPace.startsWith(":") || strPace.endsWith(":")) throw new Exception();
                String[] parts = strPace.split(":");
                int min = Integer.parseInt(parts[0]);
                int sec = Integer.parseInt(parts[1]);
                if (sec >= 60 || parts[1].length() >= 3) throw new Exception();

                int totalSecondsPace = (min * 60) + sec;

                // 🌟 핵심: BadgeTier의 공용 메서드 호출
                String tierKey = BadgeTier.getTierNameByRecord(km, totalSecondsPace);
                BadgeTier tier = BadgeTier.fromString(tierKey);

                tvCalcResult.setText("등급 : " + tier.getName());
                tvCalcResult.setTextColor(tier.getColor());

            } catch (Exception e) {
                tvCalcResult.setText("입력 오류");
                tvCalcResult.setTextColor(Color.parseColor("#FF0000"));
            }
        });

        // 서버 데이터 연동
        BadgeService service = ApiClient.getInstance().create(BadgeService.class);
        BadgeRepository repository = new BadgeRepository(service);
        repository.fetchBadgeDetail(this::updateBadgeUI);
        /*BadgeService service = MockApiClient.getInstance().create(BadgeService.class);
        BadgeRepository repository = new BadgeRepository(service);임시 목서버*/
    }

    // ─── NONE 등급일 때 1km 이상 기록 중 최고 페이스 기록을 서버에서 조회해 UI에 표시 ───
    private void loadBestRecord() {
        int userId = TokenManager.getUserId(this);
        RunningRepository runningRepo = new RunningRepository();
        runningRepo.fetchUserRecords(userId, new RunningRepository.RecordCallback() {
            @Override
            public void onSuccess(List<RunningRecordResponse> records) {
                RunningRecordResponse best = null;
                float bestPaceSec = Float.MAX_VALUE;

                for (RunningRecordResponse r : records) {
                    if (r.getDistance() < 1.0f) continue;

                    float pace = r.getPace();
                    // pace 단위 정규화: < 60이면 분/km (구 포맷) → 초/km로 변환
                    float paceSec = (pace < 60f) ? pace * 60f : pace;

                    if (paceSec < bestPaceSec) {
                        bestPaceSec = paceSec;
                        best = r;
                    }
                }

                if (best == null) return; // 조건 만족 기록 없으면 GONE 유지

                final RunningRecordResponse bestRecord = best;
                final float finalPaceSec = bestPaceSec;

                runOnUiThread(() -> {
                    int min = (int) (finalPaceSec / 60);
                    int sec = (int) (finalPaceSec % 60);
                    String paceStr = String.format("%d'%02d\"", min, sec);
                    String distStr = String.format("%.2fkm  %s/km", bestRecord.getDistance(), paceStr);

                    // 날짜: createdAt에서 앞 10자 (yyyy-MM-dd)
                    String dateRaw = bestRecord.getCreatedAt();
                    String dateStr = (dateRaw != null && dateRaw.length() >= 10)
                            ? dateRaw.substring(0, 10).replace("-", ".")
                            : "-";

                    tvBadgeRecord.setText(distStr);
                    tvBadgeRecord.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    tvBadgeRecord.setVisibility(View.VISIBLE);

                    tvBadgeDate.setText("최고 기록 : " + dateStr);
                    tvBadgeDate.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    tvBadgeDate.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(String message) {
                // 실패 시 그냥 GONE 유지
            }
        });
    }

    // ─── 서버 응답으로 받은 뱃지 정보를 바탕으로 UI(등급명·색상·기록·날짜) 갱신 ───
    private void updateBadgeUI(BadgeDetailResponse response) {
        if (response == null) return;
        BadgeTier tier = BadgeTier.fromString(response.tier);

        tvBadgeTierName.setText("Tier - " + tier.getName());
        tvBadgeTierName.setTextColor(tier.getColor());
        ivBadgeMain.setColorFilter(tier.getColor());

        if (tier == BadgeTier.NONE) {
            tvBadgeRecord.setVisibility(View.GONE);
            tvBadgeDate.setVisibility(View.GONE);
            // 언랭: 1km 이상 기록 중 최고 페이스 조회
            loadBestRecord();
        } else {
            tvBadgeRecord.setVisibility(View.VISIBLE);
            tvBadgeDate.setVisibility(View.VISIBLE);

            String record = String.format("%.2fkm  %s/km", response.distance, response.pace);
            tvBadgeRecord.setText(record);
            tvBadgeRecord.setTextColor(tier.getColor());

            tvBadgeDate.setText("달성일자 : " + response.achievedAt);
            tvBadgeDate.setTextColor(tier.getColor());
        }
    }
}