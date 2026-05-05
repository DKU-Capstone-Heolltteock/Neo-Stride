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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.ProgressBar;
import android.graphics.PorterDuff;

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
import com.neostride.app.feature.coaching.GoalStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // 🌟 1. 백엔드 통신용 Repository
    private RunningRepository runningRepository;
    private ViewPager2 viewPagerRunningMode;
    private CardView btnStop, btnPause, btnMyLocation, btnResultConfirm;
    private CardView btnGoalCompleted;
    private CardView btnWarningStop;
    private TextView btnWarningResume;
    private LinearLayout layoutResult, layoutRunningControls, layoutCoachingGoals, layoutStopWarning;
    private FrameLayout layoutStartButtons;
    private TextView tvElapsedTime, tvDistance;
    private ImageView ivPausePlay;
    private TextView tvResultDistance, tvResultTime, tvResultPace;
    private TextView tvGoalDistanceLabel, tvGoalTimeLabel;
    private ProgressBar progressGoalDistance, progressGoalTime;

    // 🌟 3. 타이머 및 상태 변수
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0, pausedDuration = 0, pauseStartTime = 0, elapsedMillis = 0;
    private boolean isRunning = false, isPaused = false, isCoachingRun = false;

    // 🌟 4. 오늘 작업한 목표 관련 변수
    private double targetDistanceKm = 3.5;
    private int targetTimeSeconds = 1299;

    // 🌟 5. 기존에 있던 지도 경로 기록 및 분석 변수들 (서버 전송 및 Polyline용)
    private LocationCallback locationCallback;
    private List<LatLng> routePoints = new ArrayList<>();
    private List<String> routeTimestamps = new ArrayList<>();
    private List<Float> segmentPaces = new ArrayList<>();
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;
    private float finalPaceMinPerKm = 0f;
    private float finalCalories = 0f;

    // GPS 필터 및 페이스 컬러 상수 (그대로 유지)
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
        // 1. 레이아웃 인플레이트
        View view = inflater.inflate(R.layout.fragment_running, container, false);

        // 2. 위치 서비스 및 레포지토리 초기화 (서버 전송을 위해 필수!)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // 🌟 [보존] 이전 코드의 서버 통신 레포지토리 설정
        RunningApi runningApi = ApiClient.getInstance().create(RunningApi.class);
        runningRepository = new RunningRepository(runningApi);

        // 3. 뷰 연결 및 초기화 (에러 해결의 핵심)
        initViews(view);           // 모든 버튼, 텍스트뷰, 프로그레스바 연결
        setupViewPager(view);      // 자유 러닝 / 오늘의 목표 모드 선택 설정
        setupLocationCallback();   // 위치 신호 처리 및 경로 그리기 준비

        // 4. 지도 설정
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        return view;
    }

    private void initViews(View v) {
        viewPagerRunningMode = v.findViewById(R.id.viewPagerRunningMode);
        layoutStartButtons = v.findViewById(R.id.layout_start_buttons);
        layoutRunningControls = v.findViewById(R.id.layout_running_controls);
        layoutResult = v.findViewById(R.id.layout_result);
        layoutCoachingGoals = v.findViewById(R.id.layout_coaching_goals);
        layoutStopWarning = v.findViewById(R.id.layout_stop_warning);
        btnWarningResume = v.findViewById(R.id.btn_warning_resume);
        btnWarningStop = v.findViewById(R.id.btn_warning_stop);
        btnStop = v.findViewById(R.id.btn_stop);
        btnPause = v.findViewById(R.id.btn_pause);
        btnMyLocation = v.findViewById(R.id.btn_my_location);
        btnResultConfirm = v.findViewById(R.id.btn_result_confirm);
        btnGoalCompleted = v.findViewById(R.id.btn_goal_completed);

        tvElapsedTime = v.findViewById(R.id.tv_elapsed_time);
        tvDistance = v.findViewById(R.id.tv_distance);
        ivPausePlay = v.findViewById(R.id.iv_pause_play); // 🌟 새로운 ID와 연결
        tvResultDistance = v.findViewById(R.id.tv_result_distance);
        tvResultTime = v.findViewById(R.id.tv_result_time);
        tvResultPace = v.findViewById(R.id.tv_result_pace);
        tvGoalDistanceLabel = v.findViewById(R.id.tv_goal_distance_label);
        tvGoalTimeLabel = v.findViewById(R.id.tv_goal_time_label);

        progressGoalDistance = v.findViewById(R.id.progress_goal_distance);
        progressGoalTime = v.findViewById(R.id.progress_goal_time);

        // 프로그레스바 최대치 설정 (정밀도를 위해 10000 사용)
        if (progressGoalDistance != null) progressGoalDistance.setMax(10000);
        if (progressGoalTime != null) progressGoalTime.setMax(10000);

        // 리스너 연결
        if (btnStop != null) btnStop.setOnClickListener(view -> checkBeforeStop());
        if (btnPause != null) btnPause.setOnClickListener(view -> togglePause());
        if (btnMyLocation != null) btnMyLocation.setOnClickListener(view -> moveToCurrentLocation(true));
        if (btnResultConfirm != null) btnResultConfirm.setOnClickListener(view -> {
            sendDataToBackend(); // 🌟 여기서 기존의 서버 전송 호출
            resetToReady();
        });
        if (btnWarningResume != null) btnWarningResume.setOnClickListener(view -> {
            layoutStopWarning.setVisibility(View.GONE);
            if (isPaused) togglePause();
        });
        if (btnWarningStop != null) btnWarningStop.setOnClickListener(view -> {
            layoutStopWarning.setVisibility(View.GONE);
            stopTracking(false);
        });
        if (btnGoalCompleted != null) btnGoalCompleted.setOnClickListener(view -> resetToReady());
    }

    private void setupViewPager(View v) {
        List<RunningModeItem> modes = new ArrayList<>();
        modes.add(new RunningModeItem("측정 시작", "자유 러닝", 0xFFCCFF00, false));

        // 오늘 날짜 코칭 목표 확인
        Calendar today = Calendar.getInstance();
        String key = today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-" + today.get(Calendar.DAY_OF_MONTH);
        GoalStorage.PlanData plan = GoalStorage.getPlan(requireContext(), key);

        if (plan != null) {
            modes.add(new RunningModeItem("측정 시작", "오늘의 목표", 0xFFFF9500, true));
            targetDistanceKm = plan.distanceKm;
            targetTimeSeconds = (int)(plan.distanceKm * plan.paceSecPerKm);
        }

        // 1. 어댑터 설정
        viewPagerRunningMode.setAdapter(new RunningModeAdapter(modes, (item, position) -> {
            if (viewPagerRunningMode.getCurrentItem() == position) {
                isCoachingRun = item.isCoaching;
                layoutStartButtons.setVisibility(View.GONE);
                layoutRunningControls.setVisibility(View.VISIBLE);
                layoutCoachingGoals.setVisibility(isCoachingRun ? View.VISIBLE : View.GONE);
                startTracking();
            }
        }));

        // 2. 페이지 변경 콜백 설정
        viewPagerRunningMode.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                if (modes.get(position).isCoaching) {
                    layoutCoachingGoals.setVisibility(View.VISIBLE);
                    tvGoalDistanceLabel.setText(String.format("%.2fkm", targetDistanceKm));
                    progressGoalDistance.setProgress(0);
                    if (targetTimeSeconds > 0) {
                        ((View) progressGoalTime.getParent()).setVisibility(View.VISIBLE);
                        tvGoalTimeLabel.setText(String.format("%02d:%02d", targetTimeSeconds / 60, targetTimeSeconds % 60));
                        progressGoalTime.setProgress(0);
                    } else {
                        ((View) progressGoalTime.getParent()).setVisibility(View.GONE);
                    }
                } else {
                    layoutCoachingGoals.setVisibility(View.GONE);
                }
            }
        });

        // 3. 카드 디자인 효과 (PageTransformer)
        float p = 55 * getResources().getDisplayMetrics().density;
        viewPagerRunningMode.setPageTransformer((page, pos) -> {
            page.setTranslationX(-(p * pos));
            page.setAlpha(1.0f);
            if (pos == 0) { page.setScaleX(1.0f); page.setScaleY(1.0f); }
            else { page.setScaleX(0.85f); page.setScaleY(0.85f); }
        });

        //  모든 설정이 끝난 후 '자동 슬라이드' 실행
        if (plan != null && !"completed".equals(plan.status)) {
            viewPagerRunningMode.postDelayed(() -> {
                if (isAdded() && viewPagerRunningMode != null) {
                    // 오늘의 목표(1번 인덱스)로 스르륵 이동
                    viewPagerRunningMode.setCurrentItem(1, true);
                }
            }, 150);
        }
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

        // [추가] GPS 잡기 전, 대한민국(남한) 중심으로 초기 위치 고정
        // 위도 35.9, 경도 127.7 / 줌 레벨 7.0f
        com.google.android.gms.maps.model.LatLng southKoreaCenter = new com.google.android.gms.maps.model.LatLng(35.9, 127.7);
        mMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(southKoreaCenter, 7.0f));

        try {
            mMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        // 이후 권한 체크 및 내 위치로 이동 로직 실행
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

    // 🌟 수정된 부분: AI 코칭 UI 업데이트 로직 삽입
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRunning || isPaused) return;
                for (Location location : locationResult.getLocations()) {
                    // GPS 정확도 필터링
                    if (location.hasAccuracy() && location.getAccuracy() > MIN_ACCURACY_FILTER) continue;

                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    if (lastLocation != null) {
                        float distance = lastLocation.distanceTo(location);
                        if (distance < MIN_DISTANCE_FILTER) continue;

                        // 🌟 1. 실시간 페이스 계산 (이게 있어야 색이 변합니다!)
                        long timeDiff = (location.getTime() - lastLocation.getTime()) / 1000;
                        if (timeDiff > 0) {
                            float speed = distance / timeDiff;
                            if (speed <= MAX_SPEED_FILTER) { // 비정상적 속도 제외
                                float segmentPace = (timeDiff / 60f) / (distance / 1000f);
                                segmentPaces.add(segmentPace); // 구간 페이스 저장
                                if (segmentPaces.size() % 10 == 0) updatePaceThresholds();
                            }
                        }

                        totalDistanceMeters += distance;

                        // 🌟 2. UI 업데이트 (코칭 모드 게이지 & 텍스트)
                        if (isCoachingRun) {
                            float curKm = totalDistanceMeters / 1000f;
                            if (curKm >= targetDistanceKm) {
                                totalDistanceMeters = (float)(targetDistanceKm * 1000);
                                handleGoalCompleted();
                                return;
                            }

                            // 남은 거리 텍스트 및 프로그레스바 갱신
                            double remainingKm = Math.max(0, targetDistanceKm - curKm);
                            tvGoalDistanceLabel.setText(String.format("%.2fkm", remainingKm));
                            int progress = (int) ((curKm / targetDistanceKm) * 10000);
                            progressGoalDistance.setProgress(progress);
                        }

                        // 데이터 저장 및 경로 그리기
                        routePoints.add(newPoint);
                        routeTimestamps.add(timeStr);
                        lastLocation = location;

                        // 🌟 3. 핵심: 실시간 알록달록 경로 그리기 호출
                        drawColoredRoute();

                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(newPoint));
                        }
                    } else {
                        // 첫 위치 수신 시
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

    // 🌟 수정된 부분: 시간 프로그레스바 연동
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || isPaused) return;
            elapsedMillis = SystemClock.elapsedRealtime() - startTime - pausedDuration;
            long totalSec = elapsedMillis / 1000;

            // 시간 표시 (기존 로직)
            long hours = totalSec / 3600; long minutes = (totalSec % 3600) / 60; long seconds = totalSec % 60;
            if (hours > 0) tvElapsedTime.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
            else tvElapsedTime.setText(String.format("%02d:%02d", minutes, seconds));

            // [추가] AI 코칭 모드 시간 게이지 업데이트
            if (isCoachingRun && targetTimeSeconds > 0) {
                long diff = targetTimeSeconds - totalSec;
                if (diff >= 0) {
                    tvGoalTimeLabel.setText(String.format("%02d:%02d", diff / 60, diff % 60));
                    progressGoalTime.getProgressDrawable().clearColorFilter();
                    int timeProgress = (int) (((double) totalSec / targetTimeSeconds) * 10000);
                    progressGoalTime.setProgress(timeProgress);
                } else {
                    // 시간 초과 시 붉은색 표시
                    long overtime = Math.abs(diff);
                    tvGoalTimeLabel.setText(String.format("-%02d:%02d", overtime / 60, overtime % 60));
                    progressGoalTime.getProgressDrawable().setColorFilter(0xFFFF4444, PorterDuff.Mode.SRC_IN);
                    progressGoalTime.setProgress(10000);
                }
            }

            // 거리 표시 (기존 로직)
            float distanceKm = totalDistanceMeters / 1000f;
            tvDistance.setText(distanceKm < 1.0f ? String.format("%.0f m", totalDistanceMeters) : String.format("%.2fkm", distanceKm));

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
        btnStop.setVisibility(View.VISIBLE); btnPause.setVisibility(View.VISIBLE);
        tvElapsedTime.setText("00 : 00"); tvDistance.setText("0 m");
    }

    private void togglePause() {
        if (isPaused) {
            isPaused = false;
            pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;
            timerHandler.post(timerRunnable);
            // 다시 뛸 때는 '일시정지' 아이콘으로
            ivPausePlay.setImageResource(R.drawable.ic_pause);
        } else {
            isPaused = true;
            pauseStartTime = SystemClock.elapsedRealtime();
            timerHandler.removeCallbacks(timerRunnable);
            // 멈췄을 때는 '재생' 아이콘으로
            ivPausePlay.setImageResource(R.drawable.ic_play);
        }
    }

    private void checkBeforeStop() {
        // 코칭 모드인데 목표 거리를 다 못 채웠다면 경고창 띄우기
        if (isCoachingRun && totalDistanceMeters < (targetDistanceKm * 1000)) {
            if (!isPaused) togglePause(); // 잠시 일시정지
            layoutStopWarning.setVisibility(View.VISIBLE); // "정말 그만둘까요?" 팝업
        } else {
            stopTracking(true); // 목표 달성했거나 자유 러닝이면 바로 결과창으로
        }
    }

    // 목표 달성 시 호출 (자동 종료 및 세리머니)
    private void handleGoalCompleted() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        fusedLocationClient.removeLocationUpdates(locationCallback);

        if (isPaused) pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;

        // UI 세리머니 (형광색으로 고정)
        int neonGreen = 0xFFCCFF00;
        tvGoalDistanceLabel.setText("0.00km");
        progressGoalDistance.getProgressDrawable().setColorFilter(neonGreen, android.graphics.PorterDuff.Mode.SRC_IN);
        progressGoalDistance.setProgress(10000);

        // 시간 게이지가 활성화되어 있다면 같이 초록색으로
        if (targetTimeSeconds > 0) {
            progressGoalTime.getProgressDrawable().setColorFilter(neonGreen, android.graphics.PorterDuff.Mode.SRC_IN);
            progressGoalTime.setProgress(10000);
        }

        // 컨트롤러 숨기고 완료 버튼 노출
        layoutRunningControls.setVisibility(View.GONE);
        if (btnGoalCompleted != null) {
            btnGoalCompleted.setVisibility(View.VISIBLE);
        }
    }

    private void stopTracking(boolean showResult) { // boolean showResult 추가!
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        stopLocationUpdates();

        if (isPaused) {
            pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;
        }

        long totalSec = (SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000;
        float distanceKm = totalDistanceMeters / 1000f;

        // 페이스 및 칼로리 계산 (기존 로직 유지)
        finalPaceMinPerKm = (distanceKm >= 0.01f) ? (totalSec / 60f) / distanceKm : 0f;
        finalCalories = 70.0f * distanceKm * 1.036f;

        // 컨트롤러 레이아웃 숨기기
        layoutRunningControls.setVisibility(View.GONE);

        if (showResult) {
            // 결과를 보여주는 경우 (정상 종료)
            layoutResult.setVisibility(View.VISIBLE);
            tvResultDistance.setText(String.format("%.2f km", distanceKm));

            if (totalSec / 3600 > 0) {
                tvResultTime.setText(String.format("%d:%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60));
            } else {
                tvResultTime.setText(String.format("%02d:%02d", totalSec / 60, totalSec % 60));
            }

            if (finalPaceMinPerKm > 0) {
                int paceMin = (int) finalPaceMinPerKm;
                int paceSec = (int) ((finalPaceMinPerKm - paceMin) * 60);
                tvResultPace.setText(String.format("%d:%02d /km", paceMin, paceSec));
            } else {
                tvResultPace.setText("--:-- /km");
            }
        } else {
            // 결과를 안 보여주는 경우 (경고창에서 중지 눌렀을 때 등)
            resetToReady();
        }
    }

    private void resetToReady() {
        // 1. 운동 종료 후의 UI 요소들 숨기기
        if (layoutResult != null) layoutResult.setVisibility(View.GONE);
        if (layoutRunningControls != null) layoutRunningControls.setVisibility(View.GONE);
        if (btnGoalCompleted != null) btnGoalCompleted.setVisibility(View.GONE);
        if (layoutStopWarning != null) layoutStopWarning.setVisibility(View.GONE); // 경고창도 혹시 모르니 숨김

        // 2. '측정 시작' 버튼이 있는 레이아웃 다시 표시하기
        if (layoutStartButtons != null) {
            layoutStartButtons.setVisibility(View.VISIBLE);
        }

        // AI 코칭 게이지 및 라벨 초기화
        if (progressGoalDistance != null) {
            progressGoalDistance.setProgress(0);
            progressGoalDistance.getProgressDrawable().clearColorFilter(); // 완료 시 적용된 초록색 필터 제거
        }
        if (progressGoalTime != null) {
            progressGoalTime.setProgress(0);
            progressGoalTime.getProgressDrawable().clearColorFilter(); // 시간 초과 시 적용된 빨간색 필터 제거
        }

        // 목표 라벨도 초기 설정값으로 복구
        if (tvGoalDistanceLabel != null) {
            tvGoalDistanceLabel.setText(String.format("%.2fkm", targetDistanceKm));
        }
        if (tvGoalTimeLabel != null) {
            tvGoalTimeLabel.setText(String.format("%02d:%02d", targetTimeSeconds / 60, targetTimeSeconds % 60));
        }

        // 4. 지도 및 러닝 데이터 초기화
        if (mMap != null) mMap.clear();
        totalDistanceMeters = 0f;
        lastLocation = null;
        routePoints.clear();
        routeTimestamps.clear();
        segmentPaces.clear();

        // 5. 상태 변수 및 메인 텍스트 리셋
        elapsedMillis = 0;
        pausedDuration = 0;
        isPaused = false;
        paceThresholdsSet = false;
        isCoachingRun = false;

        if (tvElapsedTime != null) tvElapsedTime.setText("00 : 00");
        if (tvDistance != null) tvDistance.setText("0 m");

        if (ivPausePlay != null) {
            ivPausePlay.setImageResource(R.drawable.ic_pause);
        }
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