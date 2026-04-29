package com.neostride.app.feature.running;

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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.running.api.RunningApi;
import com.neostride.app.feature.running.model.GpsTraceRequest;
import com.neostride.app.feature.running.model.RunningRecordRequest;
import com.neostride.app.feature.running.repository.RunningRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private RunningRepository runningRepository;

    private CardView btnStart, btnStop, btnPause, btnMyLocation, btnResultConfirm;
    private LinearLayout layoutResult;
    private TextView tvElapsedTime, tvDistance, tvPauseLabel;
    private TextView tvResultDistance, tvResultTime, tvResultPace;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0;
    private long pausedDuration = 0;
    private long pauseStartTime = 0;
    private long elapsedMillis = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;

    private LocationCallback locationCallback;
    private List<LatLng> routePoints = new ArrayList<>();
    private List<String> routeTimestamps = new ArrayList<>();
    private List<Float> segmentPaces = new ArrayList<>();
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;

    private float finalPaceMinPerKm = 0f;
    private float finalCalories = 0f;

    private static final float MIN_DISTANCE_FILTER = 5f;
    private static final float MIN_ACCURACY_FILTER = 20f;
    private static final float MAX_SPEED_FILTER = 12f;

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

        RunningApi runningApi = ApiClient.getInstance().create(RunningApi.class);
        runningRepository = new RunningRepository(runningApi);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

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

        btnStart.setOnClickListener(v -> startTracking());
        btnStop.setOnClickListener(v -> stopTracking());
        btnPause.setOnClickListener(v -> togglePause());
        btnMyLocation.setOnClickListener(v -> moveToCurrentLocation(true));

        btnResultConfirm.setOnClickListener(v -> {
            sendDataToBackend();
            resetToReady();
        });

        setupLocationCallback();
        return view;
    }

    private void sendDataToBackend() {
        // [수정 완료] GpsTraceRequest 리스트 조립
        List<GpsTraceRequest> traces = new ArrayList<>();
        for (int i = 0; i < routePoints.size(); i++) {
            traces.add(new GpsTraceRequest(
                    routePoints.get(i).latitude,
                    routePoints.get(i).longitude,
                    routeTimestamps.get(i)
            ));
        }

        int currentUserId = TokenManager.getUserId(requireContext());
        int durationSeconds = (int) (elapsedMillis / 1000);

        // [수정 완료] 명칭 및 생성자 인자 순서 동기화
        RunningRecordRequest request = new RunningRecordRequest(
                currentUserId,
                null,
                totalDistanceMeters / 1000f,
                durationSeconds,
                finalPaceMinPerKm,
                finalCalories,
                "", // routeDetail 초기값
                traces
        );

        Log.d("NeoStride_Backend", "=== 서버 데이터 전송 요청 ===");
        if (runningRepository != null) {
            runningRepository.saveRunningRecord(request);
        }
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

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRunning || isPaused) return;
                for (Location location : locationResult.getLocations()) {
                    if (location.hasAccuracy() && location.getAccuracy() > MIN_ACCURACY_FILTER) continue;

                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    if (lastLocation != null) {
                        float distance = lastLocation.distanceTo(location);
                        if (distance < MIN_DISTANCE_FILTER) continue;

                        long timeDiff = (location.getTime() - lastLocation.getTime()) / 1000;
                        if (timeDiff > 0) {
                            float speed = distance / timeDiff;
                            if (speed > MAX_SPEED_FILTER) continue;

                            float segmentPace = (timeDiff / 60f) / (distance / 1000f);
                            segmentPaces.add(segmentPace);
                            if (segmentPaces.size() % 10 == 0) updatePaceThresholds();
                        }

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

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMinUpdateIntervalMillis(1000).build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() { fusedLocationClient.removeLocationUpdates(locationCallback); }

    private void drawColoredRoute() {
        if (mMap == null || routePoints.size() < 2) return;
        mMap.clear();
        for (int i = 0; i < routePoints.size() - 1; i++) {
            LatLng from = routePoints.get(i); LatLng to = routePoints.get(i + 1);
            int color = (i < segmentPaces.size()) ? getPaceColor(segmentPaces.get(i)) : COLOR_NORMAL;
            mMap.addPolyline(new PolylineOptions()
                    .add(from, to)
                    .width(12f)
                    .color(color)
                    .geodesic(true)
                    .jointType(JointType.ROUND));
        }
    }

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

    private void togglePause() {
        if (isPaused) { isPaused = false; pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime; timerHandler.post(timerRunnable); tvPauseLabel.setText("||"); }
        else { isPaused = true; pauseStartTime = SystemClock.elapsedRealtime(); timerHandler.removeCallbacks(timerRunnable); tvPauseLabel.setText("▶"); }
    }

    private void stopTracking() {
        isRunning = false; timerHandler.removeCallbacks(timerRunnable); stopLocationUpdates();
        if (isPaused) pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;
        long totalSec = (SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000;
        float distanceKm = totalDistanceMeters / 1000f;

        finalPaceMinPerKm = (distanceKm >= 0.01f) ? (totalSec / 60f) / distanceKm : 0f;
        finalCalories = 70.0f * distanceKm * 1.036f;

        btnStop.setVisibility(View.GONE); btnPause.setVisibility(View.GONE); layoutResult.setVisibility(View.VISIBLE);
        tvResultDistance.setText(String.format("%.2f km", distanceKm));
        if (totalSec / 3600 > 0) tvResultTime.setText(String.format("%d:%02d:%02d", totalSec/3600, (totalSec%3600)/60, totalSec%60));
        else tvResultTime.setText(String.format("%02d:%02d", totalSec/60, totalSec%60));

        if (finalPaceMinPerKm > 0) {
            int paceMin = (int) finalPaceMinPerKm; int paceSec = (int) ((finalPaceMinPerKm - paceMin) * 60);
            tvResultPace.setText(String.format("%d:%02d /km", paceMin, paceSec));
        } else tvResultPace.setText("--:-- /km");
    }

    private void resetToReady() {
        layoutResult.setVisibility(View.GONE); btnStart.setVisibility(View.VISIBLE);
        if (mMap != null) mMap.clear();
        totalDistanceMeters = 0f; lastLocation = null;
        routePoints.clear(); routeTimestamps.clear(); segmentPaces.clear();
        elapsedMillis = 0; pausedDuration = 0; isPaused = false; paceThresholdsSet = false;
    }

    private void checkPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                moveToCurrentLocation(false);
            }
        } else { requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE); }
    }

    private void moveToCurrentLocation(boolean isAnimate) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null && mMap != null) {
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