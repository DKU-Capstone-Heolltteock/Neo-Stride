package com.neostride.app.feature.running;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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

import java.util.ArrayList;
import java.util.List;

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // ── 뷰 ──
    private CardView btnStart, btnStop, btnPause, btnMyLocation, btnResultConfirm;
    private LinearLayout layoutResult;
    private TextView tvElapsedTime, tvDistance, tvPauseLabel;
    private TextView tvResultDistance, tvResultTime, tvResultPace;

    // ── 타이머 ──
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private long pausedDuration = 0;
    private long pauseStartTime = 0;
    private long elapsedMillis = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;

    // ── GPS 추적 ──
    private LocationCallback locationCallback;
    private List<LatLng> routePoints = new ArrayList<>();
    private List<Float> segmentPaces = new ArrayList<>();  // 각 구간의 페이스 (분/km)
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;

    // ── GPS 오차 필터 ──
    private static final float MIN_DISTANCE_FILTER = 5f;
    private static final float MIN_ACCURACY_FILTER = 20f;
    private static final float MAX_SPEED_FILTER = 12f;

    // ── 페이스별 색상 (느림 → 빠름) ──
    // 빨강(느림) → 주황 → 노랑 → 연두 → 초록(빠름)
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");  // 빨강
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");  // 주황
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");  // 노랑
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");  // 연두
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");  // 초록

    // 페이스 기준값 (분/km) — 사용자 평균에 따라 동적으로 조정됨
    private float paceThresholdVerySlow;
    private float paceThresholdSlow;
    private float paceThresholdFast;
    private float paceThresholdVeryFast;
    private boolean paceThresholdsSet = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_running, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // ── 뷰 바인딩 ──
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

        // ── 클릭 리스너 ──
        btnStart.setOnClickListener(v -> startTracking());
        btnStop.setOnClickListener(v -> stopTracking());
        btnPause.setOnClickListener(v -> togglePause());
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation(true));
        btnResultConfirm.setOnClickListener(v -> resetToReady());

        setupLocationCallback();

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        checkPermissionAndMoveCamera();
    }

    // ════════════════════════════════════════
    // 페이스 → 색상 변환
    // ════════════════════════════════════════

    private int getPaceColor(float paceMinPerKm) {
        // 아직 기준값이 설정 안 됐으면 기본값 사용
        if (!paceThresholdsSet) {
            // 기본 기준: 7분/km 기준으로 ±1.5분 범위
            if (paceMinPerKm >= 8.5f) return COLOR_VERY_SLOW;
            if (paceMinPerKm >= 7.5f) return COLOR_SLOW;
            if (paceMinPerKm >= 6.5f) return COLOR_NORMAL;
            if (paceMinPerKm >= 5.5f) return COLOR_FAST;
            return COLOR_VERY_FAST;
        }

        // 사용자 평균 기반 동적 기준
        if (paceMinPerKm >= paceThresholdVerySlow) return COLOR_VERY_SLOW;
        if (paceMinPerKm >= paceThresholdSlow)     return COLOR_SLOW;
        if (paceMinPerKm >= paceThresholdFast)     return COLOR_NORMAL;
        if (paceMinPerKm >= paceThresholdVeryFast) return COLOR_FAST;
        return COLOR_VERY_FAST;
    }

    // 전체 평균 페이스 기준으로 ±20% 범위 설정
    private void updatePaceThresholds() {
        if (segmentPaces.size() < 5) return; // 최소 5구간은 있어야 의미 있음

        float sum = 0;
        for (float p : segmentPaces) sum += p;
        float avgPace = sum / segmentPaces.size();

        // 평균 대비 비율로 5단계 구분
        paceThresholdVerySlow = avgPace * 1.20f;  // 평균보다 20% 이상 느림
        paceThresholdSlow     = avgPace * 1.08f;  // 평균보다 8% 느림
        paceThresholdFast     = avgPace * 0.92f;  // 평균보다 8% 빠름
        paceThresholdVeryFast = avgPace * 0.80f;  // 평균보다 20% 이상 빠름
        paceThresholdsSet = true;
    }

    // ════════════════════════════════════════
    // GPS 관련
    // ════════════════════════════════════════

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRunning || isPaused) return;

                for (Location location : locationResult.getLocations()) {

                    if (location.hasAccuracy() && location.getAccuracy() > MIN_ACCURACY_FILTER) {
                        continue;
                    }

                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

                    if (lastLocation != null) {
                        float distance = lastLocation.distanceTo(location);

                        if (distance < MIN_DISTANCE_FILTER) {
                            continue;
                        }

                        long timeDiff = (location.getTime() - lastLocation.getTime()) / 1000;
                        if (timeDiff > 0) {
                            float speed = distance / timeDiff;
                            if (speed > MAX_SPEED_FILTER) {
                                continue;
                            }

                            // 이 구간의 페이스 계산 (분/km)
                            float segmentPace = (timeDiff / 60f) / (distance / 1000f);
                            segmentPaces.add(segmentPace);

                            // 일정 구간마다 기준값 재계산
                            if (segmentPaces.size() % 10 == 0) {
                                updatePaceThresholds();
                            }
                        }

                        totalDistanceMeters += distance;
                        routePoints.add(newPoint);
                        lastLocation = location;
                        drawColoredRoute();
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(newPoint));
                    } else {
                        lastLocation = location;
                        routePoints.add(newPoint);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // 구간별로 색이 다른 경로 그리기
    private void drawColoredRoute() {
        if (mMap == null || routePoints.size() < 2) return;

        mMap.clear();

        // 구간별로 polyline을 나눠서 각각 다른 색으로 그림
        for (int i = 0; i < routePoints.size() - 1; i++) {
            LatLng from = routePoints.get(i);
            LatLng to = routePoints.get(i + 1);

            // segmentPaces의 인덱스는 routePoints보다 1 작음
            // (첫 번째 점은 페이스 없이 추가되므로)
            int color;
            if (i < segmentPaces.size()) {
                color = getPaceColor(segmentPaces.get(i));
            } else {
                color = COLOR_NORMAL; // 기본
            }

            PolylineOptions segment = new PolylineOptions()
                    .add(from, to)
                    .width(12f)
                    .color(color)
                    .geodesic(true);

            mMap.addPolyline(segment);
        }
    }

    // ════════════════════════════════════════
    // 타이머
    // ════════════════════════════════════════

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || isPaused) return;

            elapsedMillis = SystemClock.elapsedRealtime() - startTime - pausedDuration;
            long totalSec = elapsedMillis / 1000;

            long hours = totalSec / 3600;
            long minutes = (totalSec % 3600) / 60;
            long seconds = totalSec % 60;

            if (hours > 0) {
                tvElapsedTime.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
            } else {
                tvElapsedTime.setText(String.format("%02d : %02d", minutes, seconds));
            }

            float distanceKm = totalDistanceMeters / 1000f;
            if (distanceKm < 1.0f) {
                tvDistance.setText(String.format("%.0f m", totalDistanceMeters));
            } else {
                tvDistance.setText(String.format("%.2fkm", distanceKm));
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    // ════════════════════════════════════════
    // 버튼 동작
    // ════════════════════════════════════════

    private void startTracking() {
        isRunning = true;
        isPaused = false;
        totalDistanceMeters = 0f;
        lastLocation = null;
        routePoints.clear();
        segmentPaces.clear();
        pausedDuration = 0;
        paceThresholdsSet = false;
        if (mMap != null) mMap.clear();

        startTime = SystemClock.elapsedRealtime();
        timerHandler.post(timerRunnable);
        startLocationUpdates();

        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        btnPause.setVisibility(View.VISIBLE);

        tvElapsedTime.setText("00 : 00");
        tvDistance.setText("0 m");
    }

    private void togglePause() {
        if (isPaused) {
            isPaused = false;
            pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;
            timerHandler.post(timerRunnable);
            tvPauseLabel.setText("||");
        } else {
            isPaused = true;
            pauseStartTime = SystemClock.elapsedRealtime();
            timerHandler.removeCallbacks(timerRunnable);
            tvPauseLabel.setText("▶");
        }
    }

    private void stopTracking() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        stopLocationUpdates();

        if (isPaused) {
            pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;
        }
        long finalMillis = SystemClock.elapsedRealtime() - startTime - pausedDuration;
        long totalSec = finalMillis / 1000;
        float distanceKm = totalDistanceMeters / 1000f;

        btnStop.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);

        tvResultDistance.setText(String.format("%.2f km", distanceKm));

        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) {
            tvResultTime.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
        } else {
            tvResultTime.setText(String.format("%02d:%02d", minutes, seconds));
        }

        if (distanceKm >= 0.01f && totalSec > 0) {
            float paceMinPerKm = (totalSec / 60f) / distanceKm;
            int paceMin = (int) paceMinPerKm;
            int paceSec = (int) ((paceMinPerKm - paceMin) * 60);
            tvResultPace.setText(String.format("%d:%02d /km", paceMin, paceSec));
        } else {
            tvResultPace.setText("--:-- /km");
        }
    }

    private void resetToReady() {
        layoutResult.setVisibility(View.GONE);
        btnStart.setVisibility(View.VISIBLE);

        if (mMap != null) mMap.clear();

        totalDistanceMeters = 0f;
        lastLocation = null;
        routePoints.clear();
        segmentPaces.clear();
        elapsedMillis = 0;
        pausedDuration = 0;
        isPaused = false;
        paceThresholdsSet = false;
    }

    // ════════════════════════════════════════
    // 위치 권한
    // ════════════════════════════════════════

    private void checkPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            moveToCurrentLocation(false);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void moveToCurrentLocation(boolean isAnimate) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (isAnimate) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        } else {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionAndMoveCamera();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isRunning) {
            timerHandler.removeCallbacks(timerRunnable);
            stopLocationUpdates();
        }
    }
}