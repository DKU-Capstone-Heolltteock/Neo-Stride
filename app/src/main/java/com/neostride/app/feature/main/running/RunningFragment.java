package com.neostride.app.feature.main.running;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import com.neostride.app.R;
import com.neostride.app.common.network.ApiClient;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.main.running.api.RunningApi;
import com.neostride.app.feature.main.running.model.GpsTraceRequest;
import com.neostride.app.feature.main.running.model.RunningRecordRequest;
import com.neostride.app.feature.main.running.repository.RunningRepository;
import com.neostride.app.feature.main.coaching.GoalStorage;
import com.neostride.app.feature.main.coaching.model.FeedbackRequest;
import com.neostride.app.feature.main.coaching.model.FeedbackResponse;
import com.neostride.app.feature.main.coaching.repository.CoachingRepository;
import com.neostride.app.feature.badge.api.BadgeService;
import com.neostride.app.feature.badge.model.BadgeTier;
import com.neostride.app.feature.badge.repository.BadgeRepository;
import android.graphics.drawable.GradientDrawable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;


//  러닝 탭 Fragment
//  <p>
//  - 자유 러닝 / AI 코칭 모드를 ViewPager2 카드로 선택한다.
//  - LocationTrackingService와 연동해 화면이 꺼진 상태에서도 GPS를 1초 단위로 수신한다.
//  - 수신한 좌표를 페이스 색상 폴리라인으로 지도에 실시간 표시하고, AI 코칭 모드에서는
//    목표 거리·시간 프로그레스바를 갱신한다.
//  - 러닝 완료 시 서버에 기록을 저장하고, 배지 등급 상승 여부를 확인해 팝업을 표시한다.

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    // ── 지도 및 위치 ──
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    // ── 레포지터리 ──
    private RunningRepository runningRepository;
    private BadgeRepository badgeRepository;
    private BadgeTier cachedCurrentTier = BadgeTier.NONE; // 현재 보유 배지 등급 캐시 (등급 상승 비교용)

    // ── UI 뷰 ──
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

    // ── 타이머 및 상태 ──
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0, pausedDuration = 0, pauseStartTime = 0, elapsedMillis = 0;
    private boolean isRunning = false, isPaused = false, isCoachingRun = false;

    // ── AI 코칭 목표 ──
    private double targetDistanceKm = 3.5;
    private int targetTimeSeconds = 1299;

    // ── 오늘 코칭 플랜 날짜 키 및 서버 plan_day_id ──
    private String todayPlanKey = "";
    private Integer todayPlanServerId = null; // 서버 plan_day_id (null이면 로컬 플랜)

    // ── GPS 경로 및 거리 집계 ──
    private List<LatLng> routePoints = new ArrayList<>();
    private List<String> routeTimestamps = new ArrayList<>();
    private List<Float> segmentPaces = new ArrayList<>();
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;
    private float finalPaceMinPerKm = 0f;
    private float finalCalories = 0f;

    // ── 화면 꺼짐/켜짐 상태 ──
    private boolean isScreenOn = true;
    private final BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                isScreenOn = false;
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                isScreenOn = true;
                // 화면 켜지면 쌓인 GPS 포인트를 한 번에 다시 그림
                if (isRunning && mMap != null) drawColoredRoute();
            }
        }
    };

    // ── GPS 필터 상수 ──
    private static final float MIN_DISTANCE_FILTER = 1f;   // 최소 이동 거리(m)
    private static final float MIN_ACCURACY_FILTER = 20f;  // 최소 정확도 허용 반경(m)
    private static final float MAX_SPEED_FILTER = 12f;     // 최대 속도 허용치(m/s, 약 43km/h)

    // ── 페이스 색상 상수 ──
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    // ── 페이스 임계값 (구간 평균 기반으로 동적 계산) ──
    private float paceThresholdVerySlow, paceThresholdSlow, paceThresholdFast, paceThresholdVeryFast;
    private boolean paceThresholdsSet = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 위치 권한 런처 등록 — 허용 즉시 지도 갱신
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted || !isAdded()) return;
                    // 허용됐을 때 지도가 이미 준비됐으면 즉시 내 위치로 이동
                    if (mMap != null) {
                        try { mMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
                        moveToCurrentLocation(false);
                    }
                    // mMap이 아직 null이면 onMapReady()에서 자동 처리됨

                    // 위치 권한 허용 직후 → 배터리 최적화 제외 요청 (한 번만)
                    requestBatteryOptimizationExemptionOnce();
                }
        );
    }

    // ─── 코칭 탭으로 이동 — MainActivity의 tab_coaching 클릭을 프로그램적으로 트리거 ───
    private void navigateToCoachingTab() {
        if (!isAdded()) return;
        View coachingTab = requireActivity().findViewById(R.id.tab_coaching);
        if (coachingTab != null) coachingTab.performClick();
    }

    // ─── 배터리 최적화 제외 요청 (백그라운드 크래쉬 방지) — 앱 설치 후 한 번만 표시 ───
    private void requestBatteryOptimizationExemptionOnce() {
        if (!isAdded() || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("neo_stride_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("battery_opt_asked", false)) return; // 이미 물어봤으면 패스

        PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;

        String pkg = requireContext().getPackageName();
        if (pm.isIgnoringBatteryOptimizations(pkg)) {
            prefs.edit().putBoolean("battery_opt_asked", true).apply();
            return; // 이미 제외돼 있으면 묻지 않음
        }

        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + pkg));
            startActivity(intent);
            prefs.edit().putBoolean("battery_opt_asked", true).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

        // 현재 배지 등급 미리 캐시 (등급 상승 비교용)
        badgeRepository = new BadgeRepository(ApiClient.getInstance().create(BadgeService.class));
        badgeRepository.fetchBadgeDetail(response -> cachedCurrentTier = BadgeTier.fromString(response.tier));

        // 3. 뷰 연결 및 초기화 (에러 해결의 핵심)
        initViews(view);           // 모든 버튼, 텍스트뷰, 프로그레스바 연결
        setupViewPager(view);      // 자유 러닝 / 오늘의 목표 모드 선택 설정

        // 4. 지도 설정
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        return view;
    }

    // ─── 모든 뷰 참조 초기화 및 버튼 클릭 리스너 등록 ───
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
            sendDataToBackend();
            checkAndShowTierUpgrade();
        });
        if (btnWarningResume != null) btnWarningResume.setOnClickListener(view -> {
            layoutStopWarning.setVisibility(View.GONE);
            if (isPaused) togglePause();
        });
        if (btnWarningStop != null) btnWarningStop.setOnClickListener(view -> {
            layoutStopWarning.setVisibility(View.GONE);
            stopTracking(false);
        });
        if (btnGoalCompleted != null) btnGoalCompleted.setOnClickListener(view -> navigateToCoachingTab());
    }

    // ─── 러닝 모드 ViewPager2 설정: 자유 러닝 / 오늘의 목표 카드 구성 및 자동 슬라이드 ───
    private void setupViewPager(View v) {
        List<RunningModeItem> modes = new ArrayList<>();
        modes.add(new RunningModeItem("측정 시작", "자유 러닝", 0xFFCCFF00, false));

        // 오늘 날짜 코칭 목표 확인
        Calendar today = Calendar.getInstance();
        todayPlanKey = today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-" + today.get(Calendar.DAY_OF_MONTH);
        GoalStorage.PlanData plan = GoalStorage.getPlan(requireContext(), todayPlanKey);
        boolean isCompletedPlan = false;

        if (plan != null) {
            targetDistanceKm = plan.distanceKm;
            targetTimeSeconds = (int)(plan.distanceKm * plan.paceSecPerKm);

            // 서버에서 가져온 플랜이면 plan_day_id 보존 (AI Coaching 라벨 표시에 필요)
            if (plan.goalId != null && plan.goalId.startsWith("goal_server_")) {
                todayPlanServerId = plan.planId;
            } else {
                todayPlanServerId = null;
            }

            isCompletedPlan = "completed".equals(plan.status);
            // 완료 상태면 카드 내용을 "목표 완료 / 코칭 탭에서 확인" 으로 (형광 초록)
            // 미완료면 기본 "측정 시작 / 오늘의 목표" (오렌지)
            if (isCompletedPlan) {
                modes.add(new RunningModeItem("목표 완료", "코칭 탭에서 확인", 0xFFCCFF00, true));
            } else {
                modes.add(new RunningModeItem("측정 시작", "오늘의 목표", 0xFFFF9500, true));
            }
        }

        // 1. 어댑터 설정 — 완료된 플랜이어도 ViewPager는 살아있어야 좌우 슬라이드 가능
        final boolean planCompleted = isCompletedPlan;
        viewPagerRunningMode.setAdapter(new RunningModeAdapter(modes, (item, position) -> {
            if (viewPagerRunningMode.getCurrentItem() != position) return;
            // 완료된 코칭 카드(1번)를 누르면 코칭 탭으로 이동
            if (planCompleted && item.isCoaching) {
                navigateToCoachingTab();
                return;
            }
            isCoachingRun = item.isCoaching;
            layoutStartButtons.setVisibility(View.GONE);
            layoutRunningControls.setVisibility(View.VISIBLE);
            layoutCoachingGoals.setVisibility(isCoachingRun ? View.VISIBLE : View.GONE);
            startTracking();
        }));

        // 2. 페이지 변경 콜백 설정 — plan 상태는 매번 storage에서 최신값 읽음 (handleGoalCompleted 후에도 대응)
        viewPagerRunningMode.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                if (modes.get(position).isCoaching) {
                    layoutCoachingGoals.setVisibility(View.VISIBLE);
                    GoalStorage.PlanData currentPlan = todayPlanKey.isEmpty() ? null
                            : GoalStorage.getPlan(requireContext(), todayPlanKey);
                    boolean isCompletedNow = currentPlan != null && "completed".equals(currentPlan.status);
                    if (isCompletedNow) {
                        renderCompletedGauges(currentPlan);
                    } else {
                        // 미완료 — 기본 표시 (게이지 비움)
                        tvGoalDistanceLabel.setText(String.format("%.2fkm", targetDistanceKm));
                        progressGoalDistance.getProgressDrawable().clearColorFilter();
                        progressGoalDistance.setProgress(0);
                        if (targetTimeSeconds > 0) {
                            ((View) progressGoalTime.getParent()).setVisibility(View.VISIBLE);
                            tvGoalTimeLabel.setText(String.format("%02d:%02d", targetTimeSeconds / 60, targetTimeSeconds % 60));
                            progressGoalTime.getProgressDrawable().clearColorFilter();
                            progressGoalTime.setProgress(0);
                        } else {
                            ((View) progressGoalTime.getParent()).setVisibility(View.GONE);
                        }
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

        //  모든 설정이 끝난 후 '자동 슬라이드' — 오늘의 목표(1번 인덱스)로 이동
        // (페이지 변경 콜백 안에서 완료 상태든 미완료든 적절히 그려줌)
        if (plan != null) {
            viewPagerRunningMode.postDelayed(() -> {
                if (isAdded() && viewPagerRunningMode != null) {
                    viewPagerRunningMode.setCurrentItem(1, true);
                }
            }, 150);
        }
    }

    // ─── 러닝 완료 후 GPS 경로·거리·페이스·칼로리·배지 등급을 서버에 전송 ───
    private void sendDataToBackend() {
        // 10m(0.01km) 미만은 저장하지 않음
        if (totalDistanceMeters < 10f) return;

        // 1. GpsTraceRequest 리스트 조립
        List<GpsTraceRequest> traces = new ArrayList<>();
        for (int i = 0; i < routePoints.size(); i++) {
            traces.add(new GpsTraceRequest(
                    routePoints.get(i).latitude,
                    routePoints.get(i).longitude,
                    routeTimestamps.get(i)
            ));
        }

        // 핵심: 티어 계산을 위해 먼저 페이스(초)를 계산합니다.
        int currentUserId = TokenManager.getUserId(requireContext());
        int durationSeconds = (int) (elapsedMillis / 1000);

        // paceSeconds를 먼저 선언해야 아래 currentBadge에서 쓸 수 있습니다.
        int paceSeconds = (int) (finalPaceMinPerKm * 60);

        // 계산된 페이스와 거리를 바탕으로 티어 획득
        String currentBadge = BadgeTier.getTierNameByRecord(totalDistanceMeters / 1000f, paceSeconds);

        // 서버 전송용 Request 객체 생성
        // 코칭 러닝이면 서버 plan_day_id 전송 → 기록탭 AI Coaching 라벨 표시 연결
        Integer planId = (isCoachingRun && todayPlanServerId != null && todayPlanServerId > 0) ? todayPlanServerId : null;

        RunningRecordRequest request = new RunningRecordRequest(
                currentUserId,
                planId,
                totalDistanceMeters / 1000f,
                durationSeconds,
                paceSeconds,
                finalCalories,
                "", // routeDetail
                traces,
                currentBadge // 🌟 서버의 community_users 테이블 badge 컬럼을 업데이트함!
        );

        Log.d("NeoStride_Backend", "=== 서버 데이터 전송 요청 (등급: " + currentBadge + ") ===");
        if (runningRepository != null) {
            runningRepository.saveRunningRecord(request);
        }
    }

    // ─── AI 코칭 목표 완료 시 서버에 피드백 생성 요청 ───
    private void requestAiFeedback() {
        if (todayPlanServerId == null || todayPlanServerId <= 0) return;

        CoachingRepository coachingRepo = new CoachingRepository();
        FeedbackRequest feedbackReq = new FeedbackRequest(
                todayPlanServerId,
                totalDistanceMeters / 1000f,
                (int) (elapsedMillis / 1000),
                finalPaceMinPerKm
        );
        coachingRepo.requestFeedback(todayPlanServerId, feedbackReq,
                new CoachingRepository.OnResultListener<FeedbackResponse>() {
                    @Override
                    public void onSuccess(FeedbackResponse r) {
                        Log.d("RunningFragment", "AI 피드백 생성 성공");
                    }
                    @Override
                    public void onError(String message) {
                        Log.e("RunningFragment", "AI 피드백 생성 실패: " + message);
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // GPS 잡기 전, 대한민국(남한) 중심으로 초기 위치 고정
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

    // ─── 페이스(분/km)에 대응하는 색상 반환 (임계값 미설정 시 고정 기준 사용) ───
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

    // ─── 누적 구간 페이스 평균으로 색상 임계값 5단계를 동적 재계산 (5포인트 이상 시 활성화) ───
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

    // 서비스로부터 위치를 받아 처리 (화면 꺼져도 1초 단위 동작)
    private final LocationTrackingService.LocationListener serviceLocationListener = location -> {
        if (!isRunning || isPaused || !isAdded()) return;

        if (location.hasAccuracy() && location.getAccuracy() > MIN_ACCURACY_FILTER) return;

        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            if (distance < MIN_DISTANCE_FILTER) return;

            // 실시간 페이스 계산
            long timeDiff = (location.getTime() - lastLocation.getTime()) / 1000;
            if (timeDiff > 0) {
                float speed = distance / timeDiff;
                if (speed <= MAX_SPEED_FILTER) {
                    float segmentPace = (timeDiff / 60f) / (distance / 1000f);
                    segmentPaces.add(segmentPace);
                    if (segmentPaces.size() % 10 == 0) updatePaceThresholds();
                }
            }

            totalDistanceMeters += distance;
            lastLocation = location;

            // UI는 메인 스레드에서 업데이트
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (isCoachingRun) {
                    float curKm = totalDistanceMeters / 1000f;
                    if (curKm >= targetDistanceKm) {
                        totalDistanceMeters = (float)(targetDistanceKm * 1000);
                        handleGoalCompleted();
                        return;
                    }
                    // 화면 켜진 상태에서만 UI 텍스트/프로그레스 갱신
                    if (isScreenOn) {
                        double remainingKm = Math.max(0, targetDistanceKm - curKm);
                        tvGoalDistanceLabel.setText(String.format("%.2fkm", remainingKm));
                        int progress = (int) ((curKm / targetDistanceKm) * 10000);
                        progressGoalDistance.setProgress(progress);
                    }
                }
                routePoints.add(newPoint);
                routeTimestamps.add(timeStr);
                // 화면 켜진 상태에서만 지도 조작 (꺼진 상태에서 Map 조작 시 NaN/Binder 크래시)
                if (isScreenOn && mMap != null) {
                    drawColoredRoute();
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(newPoint));
                }
            });
        } else {
            lastLocation = location;
            routePoints.add(newPoint);
            routeTimestamps.add(timeStr);
        }
    };

    // ─── LocationTrackingService 시작 및 위치 콜백 등록 ───
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationTrackingService.setLocationListener(serviceLocationListener);
        requireContext().startForegroundService(new Intent(requireContext(), LocationTrackingService.class));
    }

    // ─── LocationTrackingService 중지 및 위치 콜백 해제 ───
    private void stopLocationUpdates() {
        LocationTrackingService.setLocationListener(null);
        requireContext().stopService(new Intent(requireContext(), LocationTrackingService.class));
    }

    // ─── 지도를 초기화하고 구간별 페이스 색상 폴리라인을 전체 재그림 ───
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

    // 시간 프로그레스바 연동
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || isPaused) return;
            elapsedMillis = SystemClock.elapsedRealtime() - startTime - pausedDuration;
            long totalSec = elapsedMillis / 1000;

            // 시간 표시
            long hours = totalSec / 3600; long minutes = (totalSec % 3600) / 60; long seconds = totalSec % 60;
            if (hours > 0) tvElapsedTime.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
            else tvElapsedTime.setText(String.format("%02d:%02d", minutes, seconds));

            // AI 코칭 모드 시간 게이지 업데이트
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

            // 잠금화면 알림 실시간 갱신
            String notifTime = hours > 0
                    ? String.format("%d:%02d:%02d", hours, minutes, seconds)
                    : String.format("%02d:%02d", minutes, seconds);
            String notifDist = distanceKm < 1.0f
                    ? String.format("%.0fm", totalDistanceMeters)
                    : String.format("%.2fkm", distanceKm);
            String notifPace;
            if (totalDistanceMeters > 100f && totalSec > 0) {
                float paceMinPerKm = (totalSec / 60f) / (totalDistanceMeters / 1000f);
                int pm = (int) paceMinPerKm;
                int ps = (int) ((paceMinPerKm - pm) * 60);
                notifPace = String.format("%d'%02d\"", pm, ps);
            } else {
                notifPace = "--'--\"";
            }
            if (isAdded()) LocationTrackingService.updateNotification(requireContext(), notifTime, notifDist, notifPace);

            timerHandler.postDelayed(this, 1000);
        }
    };

    // ─── 러닝 시작: 상태 초기화, 즉시 알림 표시, 타이머·GPS 시작 ───
    private void startTracking() {
        isRunning = true; isPaused = false; totalDistanceMeters = 0f; lastLocation = null;
        routePoints.clear(); routeTimestamps.clear(); segmentPaces.clear();
        pausedDuration = 0; paceThresholdsSet = false;
        if (mMap != null) mMap.clear();
        startTime = SystemClock.elapsedRealtime();
        LocationTrackingService.postImmediateNotification(requireContext()); // 즉시 알림 표시
        timerHandler.post(timerRunnable);
        startLocationUpdates();
        btnStop.setVisibility(View.VISIBLE); btnPause.setVisibility(View.VISIBLE);
        tvElapsedTime.setText("00 : 00"); tvDistance.setText("0 m");
    }


    // ─── 이번 러닝 등급과 캐시된 등급을 비교해 상승 시 등급 상승 팝업 표시 (자유 러닝용 - 닫으면 초기화) ───
    private void checkAndShowTierUpgrade() {
        float distanceKm = totalDistanceMeters / 1000f;
        int paceSeconds = (int) (finalPaceMinPerKm * 60);
        BadgeTier newTier = BadgeTier.fromString(BadgeTier.getTierNameByRecord(distanceKm, paceSeconds));

        if (newTier != BadgeTier.NONE && newTier.ordinal() > cachedCurrentTier.ordinal()) {
            showTierUpgradeDialog(cachedCurrentTier, newTier, true);
            cachedCurrentTier = newTier;
        } else {
            resetToReady();
        }
    }

    // ─── 코칭 목표 완료 즉시 자동 호출 - 등급 상승이 있을 때만 다이얼로그 표시 (UI는 유지) ───
    private void autoShowTierUpgradeIfAny() {
        float distanceKm = totalDistanceMeters / 1000f;
        int paceSeconds = (int) (finalPaceMinPerKm * 60);
        BadgeTier newTier = BadgeTier.fromString(BadgeTier.getTierNameByRecord(distanceKm, paceSeconds));

        if (newTier != BadgeTier.NONE && newTier.ordinal() > cachedCurrentTier.ordinal()) {
            BadgeTier oldTier = cachedCurrentTier;
            cachedCurrentTier = newTier;
            showTierUpgradeDialog(oldTier, newTier, false); // 닫아도 초기화 X — 완료 UI 유지
        }
        // 등급 변동 없으면 아무것도 하지 않음 (완료 UI 그대로 유지)
    }

    // ─── 이전·새 등급을 pill 배경으로 표시하는 등급 상승 축하 다이얼로그 ───
    // resetOnDismiss=true: 확인 클릭 시 resetToReady() 실행 (자유 러닝)
    // resetOnDismiss=false: 확인 클릭 시 다이얼로그만 닫음 (코칭 자동 호출)
    private void showTierUpgradeDialog(BadgeTier oldTier, BadgeTier newTier, boolean resetOnDismiss) {
        if (!isAdded()) return;

        android.view.View dialogView = android.view.LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_dialog_tier_upgrade, null);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 배지 아이콘 색상 = 새 등급 색
        ImageView ivIcon = dialogView.findViewById(R.id.iv_tier_icon);
        ivIcon.setColorFilter(newTier.getColor(), PorterDuff.Mode.SRC_IN);

        // 이전 등급: 해당 등급 색 + 반투명 pill 배경
        TextView tvOld = dialogView.findViewById(R.id.tv_old_tier);
        tvOld.setText(oldTier.getName());
        tvOld.setTextColor(oldTier.getColor());
        GradientDrawable oldBg = new GradientDrawable();
        oldBg.setShape(GradientDrawable.RECTANGLE);
        oldBg.setCornerRadius(40f);
        int oc = oldTier.getColor();
        oldBg.setColor(Color.argb(40, Color.red(oc), Color.green(oc), Color.blue(oc)));
        oldBg.setStroke(2, oc);
        tvOld.setBackground(oldBg);

        // 새 등급: 더 밝고 크게 + pill 배경
        TextView tvNew = dialogView.findViewById(R.id.tv_new_tier);
        tvNew.setText(newTier.getName());
        tvNew.setTextColor(newTier.getColor());
        GradientDrawable newBg = new GradientDrawable();
        newBg.setShape(GradientDrawable.RECTANGLE);
        newBg.setCornerRadius(40f);
        int nc = newTier.getColor();
        newBg.setColor(Color.argb(55, Color.red(nc), Color.green(nc), Color.blue(nc)));
        newBg.setStroke(3, nc);
        tvNew.setBackground(newBg);

        dialogView.findViewById(R.id.btn_tier_confirm).setOnClickListener(v -> {
            dialog.dismiss();
            if (resetOnDismiss) resetToReady();
        });

        dialog.show();
    }

    // ─── 일시정지/재개 토글 및 일시정지 누적 시간 계산 ───
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

    // ─── 정지 전 검사: AI 코칭 중 목표 미달이면 경고창 표시, 아니면 바로 종료 ───
    private void checkBeforeStop() {
        // 코칭 모드인데 목표 거리를 다 못 채웠다면 경고창 띄우기
        if (isCoachingRun && totalDistanceMeters < (targetDistanceKm * 1000)) {
            if (!isPaused) togglePause(); // 잠시 일시정지
            layoutStopWarning.setVisibility(View.VISIBLE); // "정말 그만둘까요?" 팝업
        } else {
            stopTracking(true); // 목표 달성했거나 자유 러닝이면 바로 결과창으로
        }
    }

    // ─── AI 코칭 목표 달성 시 타이머·GPS 중지 후 완료 UI 표시 및 백엔드 전송 ───
    private void handleGoalCompleted() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        stopLocationUpdates();

        if (isPaused) pausedDuration += SystemClock.elapsedRealtime() - pauseStartTime;

        // 페이스·칼로리 계산 (sendDataToBackend에서 finalPaceMinPerKm·finalCalories 사용)
        long totalSec = (SystemClock.elapsedRealtime() - startTime - pausedDuration) / 1000;
        elapsedMillis  = totalSec * 1000;
        float distanceKm = totalDistanceMeters / 1000f;
        finalPaceMinPerKm = (distanceKm >= 0.01f) ? (totalSec / 60f) / distanceKm : 0f;
        finalCalories     = 70.0f * distanceKm * 1.036f;

        // 오늘 플랜 상태를 completed로 저장 → 재진입 시 완료 상태 유지
        if (!todayPlanKey.isEmpty()) {
            GoalStorage.PlanData todayPlan = GoalStorage.getPlan(requireContext(), todayPlanKey);
            if (todayPlan != null) {
                todayPlan.status = "completed";
                todayPlan.completedElapsedSec = totalSec;
                GoalStorage.savePlan(requireContext(), todayPlanKey, todayPlan);

                // 서버에도 완료 상태 반영 → CoachingFragment가 덮어쓰지 않도록
                if (todayPlan.goalId != null && todayPlan.goalId.startsWith("goal_server_")) {
                    try {
                        int serverGoalId = Integer.parseInt(todayPlan.goalId.replace("goal_server_", ""));
                        com.neostride.app.feature.main.coaching.repository.CoachingRepository coachingRepo =
                                new com.neostride.app.feature.main.coaching.repository.CoachingRepository();
                        coachingRepo.updateGoalStatus(
                                serverGoalId,
                                new com.neostride.app.feature.main.coaching.model.GoalStatusUpdateRequest(false, true),
                                new com.neostride.app.feature.main.coaching.repository.CoachingRepository.OnResultListener<com.neostride.app.feature.main.coaching.model.GoalResponse>() {
                                    @Override
                                    public void onSuccess(com.neostride.app.feature.main.coaching.model.GoalResponse r) {
                                        Log.d("RunningFragment", "서버 목표 상태 완료 처리 성공");
                                    }
                                    @Override
                                    public void onError(String message) {
                                        Log.e("RunningFragment", "서버 목표 상태 완료 처리 실패: " + message);
                                    }
                                }
                        );
                    } catch (NumberFormatException e) {
                        Log.e("RunningFragment", "goalId 파싱 실패: " + todayPlan.goalId);
                    }
                }
            }
        }

        // 백엔드로 기록 전송 — 등급 상승 체크는 btnGoalCompleted 클릭 시점으로 이동
        // (checkAndShowTierUpgrade가 내부적으로 resetToReady()를 호출해 완료 UI를 초기화시키는 버그 방지)
        sendDataToBackend();

        // AI 피드백 생성 요청 — 서버 plan_day_id가 있는 코칭 러닝에서만 동작
        requestAiFeedback();

        // ── UI 세리머니 ──
        int neonGreen = 0xFFCCFF00;
        tvGoalDistanceLabel.setText("0.00km");
        progressGoalDistance.getProgressDrawable().setColorFilter(neonGreen, android.graphics.PorterDuff.Mode.SRC_IN);
        progressGoalDistance.setProgress(10000);

        // 시간 게이지: 시간 초과 상태였으면 빨간색 + "-MM:SS" 유지, 시간 내 완료면 형광 초록
        if (targetTimeSeconds > 0) {
            boolean isOvertime = totalSec > targetTimeSeconds;
            if (isOvertime) {
                // 초과 상태를 명시적으로 다시 그려준다 (타이머가 마지막에 못 그렸을 수도 있음)
                long overtime = totalSec - targetTimeSeconds;
                tvGoalTimeLabel.setText(String.format("-%02d:%02d", overtime / 60, overtime % 60));
                progressGoalTime.getProgressDrawable().setColorFilter(0xFFFF4444, android.graphics.PorterDuff.Mode.SRC_IN);
                progressGoalTime.setProgress(10000);
            } else {
                tvGoalTimeLabel.setText("00:00");
                progressGoalTime.getProgressDrawable().setColorFilter(neonGreen, android.graphics.PorterDuff.Mode.SRC_IN);
                progressGoalTime.setProgress(10000);
            }
        }

        // 컨트롤러 숨기고 ViewPager 다시 표시 — 오늘의 목표 카드를 "목표 완료" 카드로 갱신
        layoutRunningControls.setVisibility(View.GONE);
        if (layoutStartButtons != null) layoutStartButtons.setVisibility(View.VISIBLE);
        if (btnGoalCompleted != null) btnGoalCompleted.setVisibility(View.GONE);

        // 어댑터를 다시 빌드해 1번 카드를 "목표 완료 / 코칭 탭에서 확인" 으로 교체
        rebuildViewPagerAsCompleted();

        // 목표 완료 시점에 등급 상승 다이얼로그 자동 표시 (완료 UI는 그대로 유지)
        autoShowTierUpgradeIfAny();
    }

    // ─── 라이브 완료 후 ViewPager 카드를 완료 상태로 교체 (setupViewPager와 동일 구조) ───
    private void rebuildViewPagerAsCompleted() {
        if (viewPagerRunningMode == null) return;
        List<RunningModeItem> modes = new ArrayList<>();
        modes.add(new RunningModeItem("측정 시작", "자유 러닝", 0xFFCCFF00, false));
        modes.add(new RunningModeItem("목표 완료", "코칭 탭에서 확인", 0xFFCCFF00, true));

        viewPagerRunningMode.setAdapter(new RunningModeAdapter(modes, (item, position) -> {
            if (viewPagerRunningMode.getCurrentItem() != position) return;
            if (item.isCoaching) {
                // 완료된 코칭 카드 → 코칭 탭으로 이동
                navigateToCoachingTab();
            } else {
                // 자유 러닝 카드 → 자유 러닝 시작
                isCoachingRun = false;
                layoutStartButtons.setVisibility(View.GONE);
                layoutRunningControls.setVisibility(View.VISIBLE);
                layoutCoachingGoals.setVisibility(View.GONE);
                startTracking();
            }
        }));

        viewPagerRunningMode.postDelayed(() -> {
            if (isAdded() && viewPagerRunningMode != null) {
                viewPagerRunningMode.setCurrentItem(1, false); // 코칭 카드 위치
            }
        }, 100);
    }

    // ─── 완료 상태 게이지 렌더링: 거리 100% 초록, 시간은 초과면 빨강 / 시간 내면 초록 ───
    private void renderCompletedGauges(GoalStorage.PlanData plan) {
        int neonGreen = 0xFFCCFF00;

        // 거리 게이지: 완료(형광 초록 100%)
        tvGoalDistanceLabel.setText("0.00km");
        progressGoalDistance.getProgressDrawable().setColorFilter(neonGreen, android.graphics.PorterDuff.Mode.SRC_IN);
        progressGoalDistance.setProgress(10000);

        // 시간 게이지: 초과면 빨강 + "-MM:SS", 시간 내 완료면 형광 초록 + "00:00"
        if (targetTimeSeconds > 0) {
            ((View) progressGoalTime.getParent()).setVisibility(View.VISIBLE);
            long overtime = plan.completedElapsedSec - targetTimeSeconds;
            if (plan.completedElapsedSec > 0 && overtime > 0) {
                tvGoalTimeLabel.setText(String.format("-%02d:%02d", overtime / 60, overtime % 60));
                progressGoalTime.getProgressDrawable().setColorFilter(0xFFFF4444, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                tvGoalTimeLabel.setText("00:00");
                progressGoalTime.getProgressDrawable().setColorFilter(neonGreen, android.graphics.PorterDuff.Mode.SRC_IN);
            }
            progressGoalTime.setProgress(10000);
        }
    }

    // ─── 러닝 종료: 페이스·칼로리 계산 후 showResult=true이면 결과 화면, false이면 초기화 ───
    private void stopTracking(boolean showResult) {
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

        // 10m 미만이면 결과 화면 없이 바로 초기화
        if (totalDistanceMeters < 10f) {
            resetToReady();
            return;
        }

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

    // ─── 결과·컨트롤·경고 UI 숨기고 초기 '측정 시작' 화면으로 완전 복원 ───
    private void resetToReady() {
        // 1. 운동 종료 후의 UI 요소들 숨기기
        if (layoutResult != null) layoutResult.setVisibility(View.GONE);
        if (layoutRunningControls != null) layoutRunningControls.setVisibility(View.GONE);
        if (btnGoalCompleted != null) btnGoalCompleted.setVisibility(View.GONE);
        if (layoutStopWarning != null) layoutStopWarning.setVisibility(View.GONE); // 경고창도 혹시 모르니 숨김

        // '측정 시작' 버튼이 있는 레이아웃 다시 표시하기
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

        // 지도 및 러닝 데이터 초기화
        if (mMap != null) mMap.clear();
        totalDistanceMeters = 0f;
        lastLocation = null;
        routePoints.clear();
        routeTimestamps.clear();
        segmentPaces.clear();

        // 상태 변수 및 메인 텍스트 리셋
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

    // ─── 위치 권한이 이미 있으면 내 위치로 이동, 없으면 onCreateView에서 이미 요청 중 ───
    private void checkPermissionAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                try { mMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
                moveToCurrentLocation(false);
            }
        }
        // 권한 없을 때는 onCreateView()의 locationPermissionLauncher가 이미 요청함
        // → 허용되면 런처 콜백에서 즉시 지도 갱신
    }

    // ─── 현재 위치를 1회 조회해 지도 카메라를 이동 (isAnimate=true이면 애니메이션) ───
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
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        requireContext().registerReceiver(screenStateReceiver, filter);

        // 오늘의 코칭 목표를 워치로 전송 (워치 코칭 모드 버튼 표시 여부 결정)
        WearGoalSender.sendTodayGoal(requireContext());

        // 위치 권한 요청 — onResume에서 처리해야 알림 권한 다이얼로그가 닫힌 뒤 순서대로 뜸
        // (onCreateView에서 요청하면 알림 다이얼로그와 충돌해 Android가 무시함)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            // 위치 권한이 이미 있는 경우 → 바로 배터리 최적화 요청 (한 번만)
            requestBatteryOptimizationExemptionOnce();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try { requireContext().unregisterReceiver(screenStateReceiver); } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isRunning) { timerHandler.removeCallbacks(timerRunnable); stopLocationUpdates(); }
    }
}