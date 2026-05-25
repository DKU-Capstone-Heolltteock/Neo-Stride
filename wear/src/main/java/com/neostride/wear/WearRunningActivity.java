package com.neostride.wear;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WearRunningActivity extends FragmentActivity {

    private static final String PATH_COACHING_GOAL = "/coaching_goal";

    // UI
    private TextView tvDistance, tvTime, tvPace, tvBtnLabel;
    private TextView tvResultDistance, tvResultTime, tvResultPace, tvResultLabel;
    private TextView tvGoalInfo, tvCoachingRemaining, tvGoalTimeInfo;
    private CardView btnToggle, btnResultConfirm, btnFreeRun, btnCoachingRun;
    private View layoutRunning, layoutResult, layoutModePicker;

    // 상태
    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isCoachingMode = false;
    private boolean goalReached = false; // 목표 거리를 실제로 달성했을 때만 true

    // 코칭 목표 거리
    private float targetDistanceKm = 0f;

    // 측정값
    private float totalDistanceKm = 0f;
    private int elapsedSec = 0;
    private double lastLat = 0, lastLng = 0;
    private long lastTime = 0;
    private boolean hasLastLocation = false;
   
    private boolean isGpsAcquired = false;
    private final List<double[]> gpsPoints = new ArrayList<>();

    // 타이머
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // 권한
    private ActivityResultLauncher<String> locationPermissionLauncher;

    // GPS 브로드캐스트 수신
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(WearLocationService.EXTRA_PERMISSION_DENIED, false)) {
                stopRunning();
                Toast.makeText(WearRunningActivity.this, "위치 권한이 없습니다.\n설정에서 허용해 주세요.", Toast.LENGTH_LONG).show();
                tvBtnLabel.setText("시작");
                return;
            }

            if (!isRunning || isPaused) return;

            double lat = intent.getDoubleExtra(WearLocationService.EXTRA_LATITUDE, 0);
            double lng = intent.getDoubleExtra(WearLocationService.EXTRA_LONGITUDE, 0);
            long time = intent.getLongExtra(WearLocationService.EXTRA_TIME, 0);
            float accuracy = intent.getFloatExtra(WearLocationService.EXTRA_ACCURACY, 999f);

            // 정확도 15m 초과 좌표 제거
            if (accuracy > 15f) return;

            if (!isGpsAcquired) {
                isGpsAcquired = true;
                tvPace.setText("--'--\"/km");
                // 첫 좌표는 버리고 기준점으로만 사용 (초반 튐 방지)
                lastLat = lat;
                lastLng = lng;
                lastTime = time;
                hasLastLocation = true;
                return;
            }

            if (hasLastLocation) {
                float[] result = new float[1];
                android.location.Location.distanceBetween(lastLat, lastLng, lat, lng, result);
                float distMeters = result[0];

                // 1m 미만 이동 무시
                if (distMeters < 1f) return;

                // 속도 43km/h 초과 좌표 제거 (오차 튐 방지)
                long timeDiffSec = (time - lastTime) / 1000;
                if (timeDiffSec > 0) {
                    float speedKmh = (distMeters / timeDiffSec) * 3.6f;
                    if (speedKmh > 43f) return;
                }

                totalDistanceKm += distMeters / 1000f;
                updateDistanceUI();

                // 코칭 모드: 목표 거리 달성 시 자동 종료
                if (isCoachingMode && targetDistanceKm > 0 && totalDistanceKm >= targetDistanceKm) {
                    totalDistanceKm = targetDistanceKm; // 정확히 목표치로 고정
                    updateDistanceUI();
                    goalReached = true;
                    stopRunning();
                    return;
                }

                // 코칭 모드: 남은 거리 실시간 갱신
                if (isCoachingMode && tvCoachingRemaining != null) {
                    float remaining = Math.max(0f, targetDistanceKm - totalDistanceKm);
                    tvCoachingRemaining.setText(String.format(Locale.getDefault(), "목표까지 %.2fkm", remaining));
                }
            }

            lastLat = lat;
            lastLng = lng;
            lastTime = time;
            hasLastLocation = true;
            gpsPoints.add(new double[]{lat, lng, time});
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_running);

        // 뷰 바인딩
        tvDistance         = findViewById(R.id.tv_distance);
        tvTime             = findViewById(R.id.tv_time);
        tvPace             = findViewById(R.id.tv_pace);
        tvBtnLabel         = findViewById(R.id.tv_btn_label);
        tvResultDistance   = findViewById(R.id.tv_result_distance);
        tvResultTime       = findViewById(R.id.tv_result_time);
        tvResultPace       = findViewById(R.id.tv_result_pace);
        tvResultLabel      = findViewById(R.id.tv_result_label);
        tvGoalInfo         = findViewById(R.id.tv_goal_info);
        tvGoalTimeInfo = findViewById(R.id.tv_goal_time_info);
        tvCoachingRemaining = findViewById(R.id.tv_coaching_remaining);
        btnToggle          = findViewById(R.id.btn_toggle);
        btnResultConfirm   = findViewById(R.id.btn_result_confirm);
        btnFreeRun         = findViewById(R.id.btn_free_run);
        btnCoachingRun     = findViewById(R.id.btn_coaching_run);
        layoutRunning      = findViewById(R.id.layout_running);
        layoutResult       = findViewById(R.id.layout_result);
        layoutModePicker   = findViewById(R.id.layout_mode_picker);
        View layoutStopWarning  = findViewById(R.id.layout_stop_warning);
        CardView btnWarningStop   = findViewById(R.id.btn_warning_stop);
        CardView btnWarningCancel = findViewById(R.id.btn_warning_cancel);

        // 권한 런처
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) startRunning(); }
        );

        // 자유 러닝 버튼
        btnFreeRun.setOnClickListener(v -> {
            isCoachingMode = false;
            checkPermissionAndStart();
        });

        // 코칭 모드 버튼
        btnCoachingRun.setOnClickListener(v -> {
            isCoachingMode = true;
            checkPermissionAndStart();
        });

        // 일시정지/재개 버튼 (측정 중 화면)
        btnToggle.setOnClickListener(v -> {
            if (!isRunning) {
                // 모드 피커 없이 layout_running이 바로 보이는 경우 (코칭 목표 없을 때)
                isCoachingMode = false;
                checkPermissionAndStart();
            } else if (isPaused) {
                resumeRunning();
            } else {
                pauseRunning();
            }
        });

        // 길게 누르면 종료 전 검사 (코칭 미달이면 경고창)
        btnToggle.setOnLongClickListener(v -> {
            if (isRunning) { checkBeforeStop(layoutStopWarning); return true; }
            return false;
        });

        // 경고창 - 종료
        btnWarningStop.setOnClickListener(v -> {
            layoutStopWarning.setVisibility(View.GONE);
            stopRunning();
        });

        // 경고창 - 계속 달리기
        btnWarningCancel.setOnClickListener(v -> {
            layoutStopWarning.setVisibility(View.GONE);
            layoutRunning.setVisibility(View.VISIBLE);
            if (isPaused) resumeRunning();
        });

        // 결과 확인 버튼
        btnResultConfirm.setOnClickListener(v -> finish());

        // 폰에서 전송한 오늘의 코칭 목표 DataItem 읽기
        loadCoachingGoal();
    }

    // ─── 폰에서 전송한 /coaching_goal DataItem을 읽어 모드 선택 화면 표시 여부 결정 ───
    private void loadCoachingGoal() {
        Wearable.getDataClient(this)
                .getDataItems(Uri.parse("wear://*" + PATH_COACHING_GOAL))
                .addOnSuccessListener(dataItems -> {
                    for (DataItem item : dataItems) {
                        DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                        float distanceKm = map.getFloat("distance_km", 0f);
                        int paceSecPerKm = map.getInt("pace_sec_per_km", 0);
                        if (distanceKm > 0f) {
                            targetDistanceKm = distanceKm;
                            showModePicker(distanceKm, paceSecPerKm);
                            dataItems.release();
                            return;
                        }
                    }
                    // 코칭 목표 없음 → 자유 러닝 화면 바로 표시
                    showFreeRunOnly();
                    dataItems.release();
                })
                .addOnFailureListener(e -> showFreeRunOnly());
    }

    // 코칭 목표 있음 → 모드 선택 화면
    private void showModePicker(float distanceKm, int paceSecPerKm) {
        layoutModePicker.setVisibility(View.VISIBLE);
        layoutRunning.setVisibility(View.GONE);
        layoutResult.setVisibility(View.GONE);
        tvGoalInfo.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));

        // 예상 시간 = 거리 × 페이스(초/km)
        int totalSec = (int)(distanceKm * paceSecPerKm);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        tvGoalTimeInfo.setText(String.format(Locale.getDefault(), "%d:%02d 이내", min, sec));
    }

    // 코칭 목표 없음 → 자유 러닝 화면 바로
    private void showFreeRunOnly() {
        layoutModePicker.setVisibility(View.GONE);
        layoutRunning.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);
    }

    private void checkPermissionAndStart() {
        layoutModePicker.setVisibility(View.GONE);
        layoutRunning.setVisibility(View.VISIBLE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startRunning();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startRunning() {
        isRunning = true;
        isPaused = false;
        totalDistanceKm = 0f;
        elapsedSec = 0;
        hasLastLocation = false;
        lastTime = 0;
        isGpsAcquired = false;
        goalReached = false;
        gpsPoints.clear();

        tvBtnLabel.setText("일시정지");
        tvBtnLabel.setTextColor(0xFF000000);
        tvPace.setText("GPS 찾는 중...");

        // 코칭 모드: 남은 거리 뷰 표시
        if (tvCoachingRemaining != null) {
            if (isCoachingMode && targetDistanceKm > 0) {
                tvCoachingRemaining.setVisibility(View.VISIBLE);
                tvCoachingRemaining.setText(String.format(Locale.getDefault(), "목표까지 %.2fkm", targetDistanceKm));
            } else {
                tvCoachingRemaining.setVisibility(View.GONE);
            }
        }

        // GPS 서비스 시작
        startForegroundService(new Intent(this, WearLocationService.class));

        IntentFilter filter = new IntentFilter(WearLocationService.ACTION_LOCATION_UPDATE);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        startTimer();
    }

    private void pauseRunning() {
        isPaused = true;
        tvBtnLabel.setText("재개");
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void resumeRunning() {
        isPaused = false;
        tvBtnLabel.setText("일시정지");
        startTimer();
    }

    // 코칭 모드 목표 미달 중단 시 경고창, 그 외엔 바로 종료
    private void checkBeforeStop(View layoutStopWarning) {
        if (isCoachingMode && !goalReached && totalDistanceKm < targetDistanceKm) {
            if (!isPaused) pauseRunning();
            layoutRunning.setVisibility(View.GONE);
            layoutStopWarning.setVisibility(View.VISIBLE);
        } else {
            stopRunning();
        }
    }

    private void stopRunning() {
        isRunning = false;
        isPaused = false;

        timerHandler.removeCallbacks(timerRunnable);
        stopService(new Intent(this, WearLocationService.class));
        try { unregisterReceiver(locationReceiver); } catch (Exception ignored) {}

        int paceSecPerKm = totalDistanceKm > 0 ? (int)(elapsedSec / totalDistanceKm) : 0;

        // 코칭 모드에서 목표 거리를 채운 경우(goalReached)만 저장
        // 자유 러닝은 항상 저장, 코칭 미달 중단은 저장 안 함 (폰과 동일 정책)
        if (!isCoachingMode || goalReached) {
            WearDataSender.sendRunningResult(this, totalDistanceKm, elapsedSec, paceSecPerKm, gpsPoints, goalReached);
        }

        showResult(paceSecPerKm);
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedSec++;
                updateTimeUI();
                updatePaceUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void updateDistanceUI() {
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", totalDistanceKm));
    }

    private void updateTimeUI() {
        int min = elapsedSec / 60;
        int sec = elapsedSec % 60;
        tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
    }

    private void updatePaceUI() {
        if (totalDistanceKm > 0.05f) {
            int paceSecPerKm = (int)(elapsedSec / totalDistanceKm);
            int paceMin = paceSecPerKm / 60;
            int paceSec = paceSecPerKm % 60;
            tvPace.setText(String.format(Locale.getDefault(), "%d'%02d\"/km", paceMin, paceSec));
        }
    }

    private void showResult(int paceSecPerKm) {
        layoutRunning.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);

        // 목표 거리를 실제로 달성했을 때만 "목표 달성!", 그 외엔 "측정 완료"
        if (tvResultLabel != null) {
            if (goalReached) {
                tvResultLabel.setText("목표 달성!");
                tvResultLabel.setTextColor(0xFFFF9500);
            } else {
                tvResultLabel.setText("측정 완료");
                tvResultLabel.setTextColor(0xFFCCFF00);
            }
        }

        tvResultDistance.setText(String.format(Locale.getDefault(), "%.2f km", totalDistanceKm));

        int min = elapsedSec / 60;
        int sec = elapsedSec % 60;
        tvResultTime.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));

        int paceMin = paceSecPerKm / 60;
        int paceSec = paceSecPerKm % 60;
        tvResultPace.setText(String.format(Locale.getDefault(), "%d'%02d\"/km", paceMin, paceSec));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        try { unregisterReceiver(locationReceiver); } catch (Exception ignored) {}
        stopService(new Intent(this, WearLocationService.class));
    }
}