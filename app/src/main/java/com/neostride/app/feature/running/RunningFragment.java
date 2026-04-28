package com.neostride.app.feature.running;

// ============================================================
// RunningFragment.java
// 실시간 러닝 측정, 경로 시각화 및 데이터 수집 담당 프래그먼트

// [주요 기능]
// 1. GPS 기반 실시간 위치 추적 및 구글 맵 경로 표시 (페이스별 색상 차별화)
// 2. 주행 거리, 경과 시간 실시간 타이머 및 칼로리 계산
// 3. 측정 완료 후 데이터를 DTO(RunningRecordRequest) 구조로 조립하여 전송 준비
//
// [사용법]
// - startTracking(): 러닝 측정 시작 (GPS 및 타이머 활성화)
// - stopTracking(): 러닝 측정 종료 및 결과 레이아웃 표시
// - sendDataToBackend(): 수집된 모든 좌표와 통계 데이터를 서버로 전송 시도
// ============================================================

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.neostride.app.R;
import com.neostride.app.feature.running.dto.GpsTraceRequest;
import com.neostride.app.feature.running.dto.RunningRecordRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // ── UI 뷰 변수 ──
    private CardView btnStart, btnStop, btnPause, btnMyLocation, btnResultConfirm;
    private LinearLayout layoutResult;
    private TextView tvElapsedTime, tvDistance, tvPauseLabel;
    private TextView tvResultDistance, tvResultTime, tvResultPace;

    // ── 타이머 및 측정 상태 변수 ──
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private long pausedDuration = 0;
    private long pauseStartTime = 0;
    private long elapsedMillis = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;

    // ── GPS 및 경로 데이터 변수 ──
    private LocationCallback locationCallback;
    private List<LatLng> routePoints = new ArrayList<>();
    private List<String> routeTimestamps = new ArrayList<>(); // 좌표별 시간 기록 리스트
    private List<Float> segmentPaces = new ArrayList<>();      // 구간별 페이스 리스트
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;

    private float finalPaceMinPerKm = 0f;
    private float finalCalories = 0f;

    // ── GPS 필터 설정 (값 튀는 현상 방지) ──
    private static final float MIN_DISTANCE_FILTER = 5f;   // 최소 5미터 이동 시 기록
    private static final float MIN_ACCURACY_FILTER = 20f;  // 오차 범위 20m 이내만 수용
    private static final float MAX_SPEED_FILTER = 12f;     // 초속 12m(시속 43km) 이상 제외

    // ── 페이스별 경로 색상 정의 ──
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    private float paceThresholdVerySlow, paceThresholdSlow, paceThresholdFast, paceThresholdVeryFast;
    private boolean paceThresholdsSet = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_running, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // 뷰 바인딩
        btnStart = view.findViewById(R.id.btn_start);
        btnStop = view.findViewById(R.id.btn_stop);
        btnPause = view.findViewById(R.id.btn_pause);
        btnMyLocation = view.findViewById(R.id.btn_my_location);
        btnResultConfirm = view.findViewById(R.id.btn_result_confirm);
        layoutResult = view.findViewById(R.id.layout_result);
        tvElapsedTime = view.findViewById(R.id.tv_elapsed_time);
        tvDistance = view.findViewById(R.id.tv_distance);
        tvPauseLabel = view.findViewById(R.id.tv_pause_label);
        tvResultDistance = view.findViewById(R.id.tv_result_distance);
        tvResultTime = view.findViewById(R.id.tv_result_time);
        tvResultPace = view.findViewById(R.id.tv_result_pace);

        // 클릭 리스너 설정
        btnStart.setOnClickListener(v -> startTracking());
        btnStop.setOnClickListener(v -> stopTracking());
        btnPause.setOnClickListener(v -> togglePause());
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation(true));

        btnResultConfirm.setOnClickListener(v -> {
            sendDataToBackend(); // 데이터 전송 실행
            resetToReady();      // 화면 초기화
        });

        setupLocationCallback();
        return view;
    }

    // [백엔드 작업] 수집된 데이터를 DTO에 담아 전송 준비 (Log 출력)
    private void sendDataToBackend() {
        List<GpsTraceRequest> traces = new ArrayList<>();
        for (int i = 0; i < routePoints.size(); i++) {
            traces.add(new GpsTraceRequest(
                    routePoints.get(i).latitude,
                    routePoints.get(i).longitude,
                    routeTimestamps.get(i)
            ));
        }

        RunningRecordRequest request = new RunningRecordRequest(
                1, // TODO: 로그인 성공 후 세션의 user_id 연동
                null, // TODO: 코칭 탭에서 선택한 plan_id 연동
                totalDistanceMeters / 1000f,
                (elapsedMillis / 1000f),
                finalPaceMinPerKm,
                finalCalories,
                traces
        );

        Log.d("NeoStride_Backend", "=== 데이터 전송 준비 완료 ===");
        Log.d("NeoStride_Backend", "좌표 데이터 개수: " + traces.size());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        try {
            // 다크 테마 맵 스타일 설정
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }

        // 구글 기본 버튼 비활성화 (커스텀 버튼 사용)
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        checkPermissionAndMoveCamera();
    }

    // 구간 페이스에 따른 경로 색상 반환
    private int getPaceColor(float paceMinPerKm) {
        if (!paceThresholdsSet) {
            if (paceMinPerKm >= 8.5f) return COLOR_VERY_SLOW;
            if (paceMinPerKm >= 7.5f) return COLOR_SLOW;
            if (paceMinPerKm >= 6.5f) return COLOR_NORMAL;
            if (paceMinPerKm >= 5.5f) return COLOR_FAST;
            return COLOR_VERY_FAST;
        }
        if (paceMinPerKm >= paceThresholdVerySlow) return COLOR_VERY_SLOW;
        if (paceMinPerKm >= paceThresholdSlow)     return COLOR_SLOW;
        if (paceMinPerKm >= paceThresholdFast)     return COLOR_NORMAL;
        if (paceMinPerKm >= paceThresholdVeryFast) return COLOR_FAST;
        return COLOR_VERY_FAST;
    }

    // 평균 페이스 기반으로 색상 변화 기준점 동적 업데이트
    private void updatePaceThresholds() {
        if (segmentPaces.size() < 5) return;
        float sum = 0; for (float p : segmentPaces) sum += p;
        float avgPace = sum / segmentPaces.size();
        paceThresholdVerySlow = avgPace * 1.20f;
        paceThresholdSlow     = avgPace * 1.08f;
        paceThresholdFast     = avgPace * 0.92f;
        paceThresholdVeryFast = avgPace * 0.80f;
        paceThresholdsSet = true;
    }

    // 실시간 위치 정보 수신 로직
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRunning || isPaused) return;
                for (Location location : locationResult.getLocations()) {
                    // 정확도 필터링
                    if (location.hasAccuracy() && location.getAccuracy() > MIN_ACCURACY_FILTER) continue;

                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    if (lastLocation != null) {
                        float distance = lastLocation.distanceTo(location);
                        // 최소 이동 거리 확인
                        if (distance < MIN_DISTANCE_FILTER) continue;

                        long timeDiff = (location.getTime() - lastLocation.getTime()) / 1000;
                        if (timeDiff > 0) {
                            // 비정상 속도 필터링
                            float speed = distance / timeDiff;
                            if (speed > MAX_SPEED_FILTER) continue;

                            // 구간 페이스 계산 및 리스트 추가
                            float segmentPace = (timeDiff / 60f) / (distance / 1000f);
                            segmentPaces.add(segmentPace);
                            if (segmentPaces.size() % 10 == 0) updatePaceThresholds();
                        }

                        // 경로 기록 및 시각화
                        totalDistanceMeters += distance;
                        routePoints.add(newPoint);
                        routeTimestamps.add(timeStr);
                        lastLocation = location;
                        drawColoredRoute();
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(newPoint));
                    } else {
                        lastLocation = location;
                        routePoints.add(newPoint);
                        routeTimestamps.add(timeStr);
                    }
                }
            }
        };
    }

    // GPS 업데이트 요청 시작
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMinUpdateIntervalMillis(1000).build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // GPS 업데이트 중지
    private void stopLocationUpdates() { fusedLocationClient.removeLocationUpdates(locationCallback); }

    // 맵에 색상이 입혀진 경로(Polyline) 그리기
    private void drawColoredRoute() {
        if (mMap == null || routePoints.size() < 2) return;
        mMap.clear();
        for (int i = 0; i < routePoints.size() - 1; i++) {
            LatLng from = routePoints.get(i); LatLng to = routePoints.get(i + 1);
            int color = (i < segmentPaces.size()) ? getPaceColor(segmentPaces.get(i)) : COLOR_NORMAL;
            mMap.addPolyline(new PolylineOptions().add(from, to).width(12f).color(color).geodesic(true));
        }
    }

    // 1초 주기로 주행 시간 및 거리 화면 갱신
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || isPaused) return;
            elapsedMillis = SystemClock.elapsedRealtime() - startTime - pausedDuration;
            long totalSec = elapsedMillis / 1000;
            long hours = totalSec / 3600; long minutes = (totalSec % 3600) / 60; long seconds = totalSec % 60;
            if (hours > 0) tvElapsedTime.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
            else tvElapsedTime.setText(String.format("%02d : %02d", minutes, seconds));

            float distanceKm = totalDistanceMeters / 1000f;
            if (distanceKm < 1.0f) tvDistance.setText(String.format("%.0f m", totalDistanceMeters));
            else tvDistance.setText(String.format("%.2fkm", distanceKm));

            timerHandler.postDelayed(this, 1000);
        }
    };

    // 측정 시작 설정 초기화 및 시작
    private void startTracking() {
        isRunning = true; isPaused = false; totalDistanceMeters = 0f; lastLocation = null;
        routePoints.clear(); routeTimestamps.clear(); segmentPaces.clear();
        pausedDuration = 0; paceThresholdsSet = false;
        if (mMap != null) mMap.clear();
        startTime = SystemClock.elapsedRealtime();
        timerHandler.post(timerRunnable);
        startLocationUpdates();
        btnStart.setVisibility(View.GONE); btnStop.setVisibility(View.VISIBLE); btnPause.setVisibility(View.VISIBLE);
        tvElapsedTime.setText("00 : 00"); tvDistance.setText("0 m");
    }

    // 일시정지 및 재개 제어
    private void togglePause() {
        if (isPaused) { isPaused = false; pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime; timerHandler.post(timerRunnable); tvPauseLabel.setText("||"); }
        else { isPaused = true; pauseStartTime = SystemClock.elapsedRealtime(); timerHandler.removeCallbacks(timerRunnable); tvPauseLabel.setText("▶"); }
    }

    // 측정 종료 및 결과 데이터 계산
    private void stopTracking() {
        isRunning = false; timerHandler.removeCallbacks(timerRunnable); stopLocationUpdates();
        if (isPaused) pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;
        long totalSec = (SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000;
        float distanceKm = totalDistanceMeters / 1000f;

        // 최종 통계값 계산
        finalPaceMinPerKm = (distanceKm >= 0.01f) ? (totalSec / 60f) / distanceKm : 0f;
        finalCalories = 70.0f * distanceKm * 1.036f; // 평균 몸무게 70kg 기준 소모 칼로리 공식

        btnStop.setVisibility(View.GONE); btnPause.setVisibility(View.GONE); layoutResult.setVisibility(View.VISIBLE);
        tvResultDistance.setText(String.format("%.2f km", distanceKm));
        if (totalSec / 3600 > 0) tvResultTime.setText(String.format("%d:%02d:%02d", totalSec/3600, (totalSec%3600)/60, totalSec%60));
        else tvResultTime.setText(String.format("%02d:%02d", totalSec/60, totalSec%60));

        if (finalPaceMinPerKm > 0) {
            int paceMin = (int) finalPaceMinPerKm; int paceSec = (int) ((finalPaceMinPerKm - paceMin) * 60);
            tvResultPace.setText(String.format("%d:%02d /km", paceMin, paceSec));
        } else tvResultPace.setText("--:-- /km");
    }

    // 측정 화면 리셋 및 대기 상태 전환
    private void resetToReady() {
        layoutResult.setVisibility(View.GONE); btnStart.setVisibility(View.VISIBLE);
        if (mMap != null) mMap.clear();
        totalDistanceMeters = 0f; lastLocation = null;
        routePoints.clear(); routeTimestamps.clear(); segmentPaces.clear();
        elapsedMillis = 0; pausedDuration = 0; isPaused = false; paceThresholdsSet = false;
    }

    // 위치 권한 확인 및 맵 세팅
    private void checkPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); moveToCurrentLocation(false);
        } else { requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE); }
    }

    // 현재 위치로 지도의 카메라를 이동
    private void moveToCurrentLocation(boolean isAnimate) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (isAnimate) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        else mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) checkPermissionAndMoveCamera();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isRunning) { timerHandler.removeCallbacks(timerRunnable); stopLocationUpdates(); }
    }
}