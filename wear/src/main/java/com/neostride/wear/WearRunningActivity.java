package com.neostride.wear;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WearRunningActivity extends FragmentActivity {

    // UI
    private TextView tvDistance, tvTime, tvPace, tvBtnLabel;
    private TextView tvResultDistance, tvResultTime, tvResultPace;
    private CardView btnToggle, btnResultConfirm;
    private View layoutRunning, layoutResult;

    // 상태
    private boolean isRunning = false;
    private boolean isPaused = false;

    // 측정값
    private float totalDistanceKm = 0f;
    private int elapsedSec = 0;
    private double lastLat = 0, lastLng = 0;
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

            if (!isGpsAcquired) {
                isGpsAcquired = true;
                tvPace.setText("--'--\"/km");
            }

            if (hasLastLocation) {
                float[] result = new float[1];
                android.location.Location.distanceBetween(lastLat, lastLng, lat, lng, result);
                totalDistanceKm += result[0] / 1000f;
                updateDistanceUI();
            }

            lastLat = lat;
            lastLng = lng;
            hasLastLocation = true;
            gpsPoints.add(new double[]{lat, lng, time});
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_running);

        // 뷰 바인딩
        tvDistance = findViewById(R.id.tv_distance);
        tvTime = findViewById(R.id.tv_time);
        tvPace = findViewById(R.id.tv_pace);
        tvBtnLabel = findViewById(R.id.tv_btn_label);
        tvResultDistance = findViewById(R.id.tv_result_distance);
        tvResultTime = findViewById(R.id.tv_result_time);
        tvResultPace = findViewById(R.id.tv_result_pace);
        btnToggle = findViewById(R.id.btn_toggle);
        btnResultConfirm = findViewById(R.id.btn_result_confirm);
        layoutRunning = findViewById(R.id.layout_running);
        layoutResult = findViewById(R.id.layout_result);

        // 권한 런처
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) startRunning(); }
        );

        // 시작/정지 버튼
        btnToggle.setOnClickListener(v -> {
            if (!isRunning) {
                checkPermissionAndStart();
            } else if (isPaused) {
                resumeRunning();
            } else {
                pauseRunning();
            }
        });

        // 완료 화면 길게 누르면 종료 (워치는 화면 작아서 길게 누르기로)
        btnToggle.setOnLongClickListener(v -> {
            if (isRunning) {
                stopRunning();
                return true;
            }
            return false;
        });

        // 결과 확인 버튼
        btnResultConfirm.setOnClickListener(v -> finish());
    }

    private void checkPermissionAndStart() {
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
        isGpsAcquired = false;
        gpsPoints.clear();

        tvBtnLabel.setText("일시정지");
        tvBtnLabel.setTextColor(0xFF000000);
        tvPace.setText("GPS 찾는 중...");

        // GPS 서비스 시작
        Intent serviceIntent = new Intent(this, WearLocationService.class);
        startForegroundService(serviceIntent);

        // GPS 브로드캐스트 등록
        IntentFilter filter = new IntentFilter(WearLocationService.ACTION_LOCATION_UPDATE);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        // 타이머 시작
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

    private void stopRunning() {
        isRunning = false;
        isPaused = false;

        // 타이머 정지
        timerHandler.removeCallbacks(timerRunnable);

        // GPS 서비스 정지
        stopService(new Intent(this, WearLocationService.class));

        try { unregisterReceiver(locationReceiver); } catch (Exception ignored) {}

        // 폰으로 데이터 전송
        int paceSecPerKm = totalDistanceKm > 0 ? (int)(elapsedSec / totalDistanceKm) : 0;
        WearDataSender.sendRunningResult(this, totalDistanceKm, elapsedSec, paceSecPerKm, gpsPoints);

        // 결과 화면 표시
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