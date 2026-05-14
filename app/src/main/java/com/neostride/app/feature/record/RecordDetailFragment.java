package com.neostride.app.feature.record;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.neostride.app.R;
import com.neostride.app.feature.feed.FeedUploadDialog;
import com.neostride.app.feature.running.model.GpsTraceRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


//  러닝 기록 상세 Fragment
//  <p>
//  - Google Maps에 GPS 경로를 페이스별 색상 폴리라인으로 그린다.
//  - 내장 {@link PaceLineView}로 시간별 페이스 변화를 색상 꺾은선 그래프로 표시한다.
//  - 지도 스냅샷을 캐시에 저장한 후 {@link FeedUploadDialog}로 피드 작성 화면을 연다.
//  - isTipMode=true 일 때는 공유 버튼이 GPS 경로 선택 확인으로 동작한다.

public class RecordDetailFragment extends Fragment implements OnMapReadyCallback {

    // ── 지도 및 기록 데이터 ──
    private GoogleMap mMap;
    private RunningRecordResponse recordData;

    // ── 피드 업로드 관련 ──
    private ActivityResultLauncher<String[]> feedPhotoPickerLauncher;
    private FeedUploadDialog feedUploadDialog;

    // ── 상태 ──
    private boolean isAnalysisExpanded = false;
    private boolean isTipMode = false;

    // ── 페이스 색상 상수 (빠름→느림 기준) ──
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    // ── 페이스 임계값 (평균 페이스 기반으로 동적 계산) ──
    private float paceThresVS, paceThresS, paceThresF, paceThresVF;
    private boolean thresSet = false;

    /** 페이스 차트의 단일 데이터 포인트 (경과 시간 라벨·페이스 값) */
    public static class PacePoint {
        public String timeStr;
        public float paceValue;
        public PacePoint(String timeStr, float paceValue) {
            this.timeStr = timeStr;
            this.paceValue = paceValue;
        }
    }

    public static RecordDetailFragment newInstance(RunningRecordResponse record, boolean isTipMode) {
        RecordDetailFragment fragment = new RecordDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("record_data", record);
        args.putBoolean("tip_mode", isTipMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedPhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && feedUploadDialog != null) {
                        feedUploadDialog.addSelectedImages(uris);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_detail, container, false);

        if (getArguments() != null) {
            recordData = (RunningRecordResponse) getArguments().getSerializable("record_data");
            isTipMode  = getArguments().getBoolean("tip_mode", false);
        }

        if (recordData != null) setupBasicUI(view);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_detail);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        setupExpandableCard(view);
        return view;
    }

    // ─── 기본 통계(날짜·거리·시간·칼로리·페이스) 텍스트뷰에 바인딩 ───
    private void setupBasicUI(View view) {
        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        if (recordData.getCreatedAt() != null) {
            tvTitle.setText(recordData.getCreatedAt().split("T")[0].replace("-", "."));
        }
        ((TextView) view.findViewById(R.id.tv_detail_distance))
                .setText(String.format(Locale.KOREA, "%.2f km", recordData.getDistance()));
        ((TextView) view.findViewById(R.id.tv_detail_time))
                .setText(formatTime((int) recordData.getTime()));
        ((TextView) view.findViewById(R.id.tv_detail_calories))
                .setText(String.valueOf((int) recordData.getCalories()));

        // pace < 60이면 구버전(분 단위), >= 60이면 신버전(초 단위)
        int paceSeconds = recordData.getPace() < 60
                ? (int) (recordData.getPace() * 60)
                : (int) recordData.getPace();
        ((TextView) view.findViewById(R.id.tv_detail_pace))
                .setText(String.format(Locale.KOREA, "%d'%02d\"", paceSeconds / 60, paceSeconds % 60));
    }

    // ─── 페이스 분석 카드 토글 설정 및 PaceLineView 생성·주입 ───
    private void setupExpandableCard(View view) {
        View toggleArea            = view.findViewById(R.id.layout_expand_toggle);
        LinearLayout expandContent = view.findViewById(R.id.layout_expand_content);
        ImageView arrowIcon        = view.findViewById(R.id.iv_expand_arrow);
        ImageView chartIcon        = view.findViewById(R.id.ivPaceChartGradient);
        ImageView routeCenterIcon  = view.findViewById(R.id.ivRouteCenterGradient);
        FrameLayout chartContainer = view.findViewById(R.id.chart_container);
        LinearLayout layoutSelectedInfo = view.findViewById(R.id.layout_selected_info);
        TextView tvSelectedTime    = view.findViewById(R.id.tv_selected_time);
        TextView tvSelectedPace    = view.findViewById(R.id.tv_selected_pace);

        int[]   colors = {COLOR_VERY_SLOW, COLOR_SLOW, COLOR_NORMAL, COLOR_FAST, COLOR_VERY_FAST};
        float[] pos    = {0f, 0.25f, 0.5f, 0.75f, 1f};
        setGradientTintToIcon(chartIcon, colors, pos);
        setGradientTintToIcon(routeCenterIcon, colors, pos);

        updatePaceThresholds();

        if (recordData != null && recordData.getGpsPath() != null) {
            PaceLineView lineView = new PaceLineView(getContext());
            lineView.setData(calculatePacePoints(recordData.getGpsPath()));
            lineView.setOnPointSelectedListener(point -> {
                layoutSelectedInfo.setVisibility(View.VISIBLE);
                tvSelectedTime.setText("시간: " + point.timeStr);
                tvSelectedPace.setText("페이스: " + formatPaceStr(point.paceValue));
            });
            chartContainer.addView(lineView);
        }

        arrowIcon.setRotation(0);
        toggleArea.setOnClickListener(v -> {
            if (isAnalysisExpanded) {
                expandContent.setVisibility(View.GONE);
                arrowIcon.animate().rotation(0).setDuration(300).start();
            } else {
                expandContent.setVisibility(View.VISIBLE);
                arrowIcon.animate().rotation(180).setDuration(300).start();
            }
            isAnalysisExpanded = !isAnalysisExpanded;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // GPS 경로 로드 전 기본 위치: 대한민국 중심
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.9, 127.7), 7.0f));

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        try { mMap.setMyLocationEnabled(true); } catch (SecurityException e) { e.printStackTrace(); }

        if (getView() != null) {
            getView().findViewById(R.id.btn_my_location).setOnClickListener(v -> {
                @SuppressLint("MissingPermission") Location loc = mMap.getMyLocation();
                if (loc != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(loc.getLatitude(), loc.getLongitude()), 16f));
                }
            });
            getView().findViewById(R.id.btn_route_center).setOnClickListener(v -> moveCameraToRoute());

            ImageView btnShare = getView().findViewById(R.id.btn_share_circle);
            if (isTipMode) {
                // 팁 모드: GPS 경로 선택 확인 버튼
                btnShare.setImageResource(R.drawable.ic_write_feed);
                btnShare.setOnClickListener(v -> {
                    Toast.makeText(requireContext(), "GPS 경로가 선택되었습니다", Toast.LENGTH_SHORT).show();
                    confirmTipGpsSelection();
                });
            } else {
                // 일반 모드: 피드 공유 버튼
                btnShare.setOnClickListener(v -> captureMapAndOpenFeedDialog());
            }
        }

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }

        if (recordData != null && recordData.getGpsPath() != null) {
            drawFullRoute(recordData.getGpsPath());
        }
    }

    // ─── 지도 스냅샷 촬영 후 피드 업로드 다이얼로그 열기 ───
    private void captureMapAndOpenFeedDialog() {
        if (mMap == null) { openFeedDialog(null); return; }
        moveCameraToRoute();
        mMap.snapshot(bitmap -> openFeedDialog(saveBitmapToCache(bitmap)));
    }

    // ─── Bitmap을 캐시 디렉터리에 PNG로 저장하고 URI 문자열 반환 ───
    private String saveBitmapToCache(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            File file = new File(requireContext().getCacheDir(), "route_map_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush(); out.close();
            return Uri.fromFile(file).toString();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // ─── FeedUploadDialog 생성 및 표시 (지도 캡처 URI 첨부) ───
    private void openFeedDialog(String routeMapUri) {
        feedUploadDialog = new FeedUploadDialog(
                requireContext(), recordData, routeMapUri,
                () -> feedPhotoPickerLauncher.launch(new String[]{"image/*"}),
                response -> Toast.makeText(requireContext(), "피드 생성 완료: " + response.getTitle(), Toast.LENGTH_SHORT).show()
        );
        feedUploadDialog.show();
    }

    // ─── GPS 경로 전체가 보이도록 지도 카메라를 경계에 맞춰 이동 ───
    private void moveCameraToRoute() {
        if (mMap == null || recordData == null
                || recordData.getGpsPath() == null || recordData.getGpsPath().isEmpty()) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (GpsTraceRequest p : recordData.getGpsPath()) {
            builder.include(new LatLng(p.getLatitude(), p.getLongitude()));
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
    }

    // ─── GPS 포인트 쌍마다 페이스를 계산해 페이스 색상 폴리라인을 지도에 그림 ───
    private void drawFullRoute(List<GpsTraceRequest> path) {
        if (path.size() < 2) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);
            LatLng from = new LatLng(p1.getLatitude(), p1.getLongitude());
            LatLng to   = new LatLng(p2.getLatitude(), p2.getLongitude());
            double dist = distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
            long   time = (parseIsoTime(p2.getTime()) - parseIsoTime(p1.getTime())) / 1000;
            int color = COLOR_NORMAL;
            if (dist > 0 && time > 0) color = getPaceColor((float) ((time / 60.0) / dist));
            mMap.addPolyline(new PolylineOptions().add(from, to).width(14f).color(color).geodesic(true).jointType(JointType.ROUND));
            builder.include(from); builder.include(to);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
    }

    //  GPS 경로에서 구간별 페이스를 계산하고 노이즈 필터링 후 PacePoint 목록을 반환한다.
    //  - 1단계: 인접값 대비 1.5배 초과/0.65배 미만 스파이크를 이웃 평균으로 대체
    //  - 2단계: 7포인트 이동평균 1회 적용
    private List<PacePoint> calculatePacePoints(List<GpsTraceRequest> path) {
        List<PacePoint> points = new ArrayList<>();
        if (path == null || path.size() < 2) return points;

        long startMillis = parseIsoTime(path.get(0).getTime());
        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);
            double dist    = distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
            long currentMs = parseIsoTime(p2.getTime());
            long diffSec   = (currentMs - parseIsoTime(p1.getTime())) / 1000;
            if (dist > 0.001 && diffSec > 0) {
                float pace   = (float) ((diffSec / 60.0) / dist);
                long elapsed = (currentMs - startMillis) / 1000;
                String label = String.format(Locale.KOREA, "%02d:%02d", elapsed / 60, elapsed % 60);
                points.add(new PacePoint(label, Math.min(pace, 15f)));
            }
        }

        if (points.size() < 3) return points;

        // 1단계: 고립된 GPS 노이즈 스파이크 제거
        for (int i = 1; i < points.size() - 1; i++) {
            float v = points.get(i).paceValue, prev = points.get(i-1).paceValue, next = points.get(i+1).paceValue;
            if ((v > prev * 1.5f && v > next * 1.5f) || (v < prev * 0.65f && v < next * 0.65f)) {
                points.get(i).paceValue = (prev + next) / 2f;
            }
        }

        // 2단계: 7포인트 이동평균
        float[] smoothed = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            int from = Math.max(0, i - 3), to = Math.min(points.size() - 1, i + 3);
            float sum = 0;
            for (int j = from; j <= to; j++) sum += points.get(j).paceValue;
            smoothed[i] = sum / (to - from + 1);
        }
        for (int i = 0; i < points.size(); i++) points.get(i).paceValue = smoothed[i];

        return points;
    }

    //  페이스 꺾은선 차트 커스텀 뷰
    //  - 구간별 페이스를 색상 세그먼트로 분할해 Canvas에 직접 그린다.
    //  - 터치 이벤트로 선택 포인트를 결정해 OnPointSelectedListener에 콜백한다.
    private class PaceLineView extends View {
        private List<PacePoint> points = new ArrayList<>();
        private final Paint linePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int selectedIndex = -1;
        private OnPointSelectedListener listener;
        private final float paddingLeft = 100f, paddingBottom = 60f, paddingTop = 40f, paddingRight = 40f;

        public PaceLineView(Context context) {
            super(context);
            linePaint.setStrokeWidth(6f); linePaint.setStrokeCap(Paint.Cap.ROUND);
            axisPaint.setColor(Color.parseColor("#44FFFFFF")); axisPaint.setStrokeWidth(2f);
            textPaint.setColor(Color.parseColor("#88FFFFFF")); textPaint.setTextSize(24f);
            guidePaint.setColor(Color.parseColor("#CCFF00"));  guidePaint.setStrokeWidth(3f);
        }

        public void setData(List<PacePoint> data) { this.points = data; invalidate(); }
        public void setOnPointSelectedListener(OnPointSelectedListener l) { this.listener = l; }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (points.size() < 2) return;

            float w = getWidth() - paddingLeft - paddingRight;
            float h = getHeight() - paddingTop - paddingBottom;

            float minPace = 15f, maxPace = 0f;
            for (PacePoint p : points) {
                if (p.paceValue < minPace) minPace = p.paceValue;
                if (p.paceValue > maxPace) maxPace = p.paceValue;
            }
            float range = Math.max(1f, maxPace - minPace);

            canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + h, axisPaint);
            canvas.drawLine(paddingLeft, paddingTop + h, paddingLeft + w, paddingTop + h, axisPaint);
            canvas.drawText(formatPaceStr(minPace), 10, paddingTop + 10, textPaint);
            canvas.drawText(formatPaceStr(maxPace), 10, paddingTop + h, textPaint);

            float stepX = w / (points.size() - 1);
            for (int i = 0; i < points.size() - 1; i++) {
                float p1 = points.get(i).paceValue, p2 = points.get(i+1).paceValue;
                float y1 = paddingTop + ((p1 - minPace) / range) * h;
                float y2 = paddingTop + ((p2 - minPace) / range) * h;
                float x1 = paddingLeft + (i * stepX), x2 = paddingLeft + ((i+1) * stepX);
                for (int s = 0; s < 4; s++) {
                    float t1 = s / 4f, t2 = (s+1) / 4f;
                    linePaint.setColor(getPaceColor(p1 + (p2 - p1) * ((t1 + t2) / 2f)));
                    canvas.drawLine(x1 + (x2-x1)*t1, y1 + (y2-y1)*t1, x1 + (x2-x1)*t2, y1 + (y2-y1)*t2, linePaint);
                }
            }

            canvas.drawText(points.get(0).timeStr, paddingLeft, paddingTop + h + 40, textPaint);
            canvas.drawText(points.get(points.size()-1).timeStr, paddingLeft + w - 60, paddingTop + h + 40, textPaint);

            if (selectedIndex != -1) {
                float selX = paddingLeft + (selectedIndex * stepX);
                canvas.drawLine(selX, paddingTop, selX, paddingTop + h, guidePaint);
                float selY = paddingTop + ((points.get(selectedIndex).paceValue - minPace) / range) * h;
                canvas.drawCircle(selX, selY, 10f, guidePaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (points.isEmpty()) return false;
            float stepX = (getWidth() - paddingLeft - paddingRight) / (points.size() - 1);
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                selectedIndex = Math.round((event.getX() - paddingLeft) / stepX);
                if (selectedIndex < 0) selectedIndex = 0;
                if (selectedIndex >= points.size()) selectedIndex = points.size() - 1;
                if (listener != null) listener.onPointSelected(points.get(selectedIndex));
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }
    }

    public interface OnPointSelectedListener { void onPointSelected(PacePoint point); }

    // ─── 평균 페이스 기반으로 색상 임계값 5단계 동적 계산 ───
    private void updatePaceThresholds() {
        if (recordData == null) return;
        // pace < 60이면 구버전(분 단위), >= 60이면 신버전(초 단위) → 분/km로 통일
        float avgPace = recordData.getPace() < 60
                ? recordData.getPace()
                : recordData.getPace() / 60f;
        paceThresVS = avgPace * 1.20f; paceThresS = avgPace * 1.08f;
        paceThresF  = avgPace * 0.92f; paceThresVF = avgPace * 0.80f;
        thresSet = true;
    }

    // ─── 페이스 값에 대응하는 색상 반환 (임계값 미설정 시 고정 기준 사용) ───
    private int getPaceColor(float pace) {
        if (!thresSet) {
            if (pace >= 8.5f) return COLOR_VERY_SLOW; if (pace >= 7.5f) return COLOR_SLOW;
            if (pace >= 6.5f) return COLOR_NORMAL;    if (pace >= 5.5f) return COLOR_FAST;
            return COLOR_VERY_FAST;
        }
        if (pace >= paceThresVS) return COLOR_VERY_SLOW; if (pace >= paceThresS) return COLOR_SLOW;
        if (pace >= paceThresF)  return COLOR_NORMAL;    if (pace >= paceThresVF) return COLOR_FAST;
        return COLOR_VERY_FAST;
    }

    // ─── ImageView 아이콘에 수평 그라데이션 색상을 SRC_ATOP 모드로 합성 ───
    private void setGradientTintToIcon(ImageView iv, int[] cls, float[] pts) {
        if (iv == null || iv.getDrawable() == null) return;
        Drawable d = iv.getDrawable();
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b); d.setBounds(0, 0, c.getWidth(), c.getHeight()); d.draw(c);
        Paint p = new Paint();
        p.setShader(new LinearGradient(0, 0, c.getWidth(), 0, cls, pts, Shader.TileMode.CLAMP));
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), p);
        iv.setImageDrawable(new BitmapDrawable(getResources(), b));
    }

    // ─── 두 위경도 좌표 사이의 거리(km) 계산 ───
    private double distanceBetween(double la1, double lo1, double la2, double lo2) {
        Location l1 = new Location(""); l1.setLatitude(la1); l1.setLongitude(lo1);
        Location l2 = new Location(""); l2.setLatitude(la2); l2.setLongitude(lo2);
        return l1.distanceTo(l2) / 1000.0;
    }

    // ─── ISO-8601 타임스탬프 문자열을 밀리초로 변환 ───
    private long parseIsoTime(String t) {
        try {
            String c = t.replace("Z", "").replace("T", " ");
            if (c.contains(".")) c = c.substring(0, c.lastIndexOf("."));
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).parse(c).getTime();
        } catch (Exception e) { return System.currentTimeMillis(); }
    }

    // ─── 팁 모드: 지도 스냅샷 후 Activity에 GPS 선택 결과 반환 ───
    private void confirmTipGpsSelection() {
        if (mMap == null) { returnTipGpsResult(null); return; }
        moveCameraToRoute();
        mMap.snapshot(bitmap -> returnTipGpsResult(saveBitmapToCache(bitmap)));
    }

    // ─── Activity.setResult로 GPS 경로 데이터 전달 후 종료 ───
    private void returnTipGpsResult(String routeMapUri) {
        Intent result = new Intent();
        result.putExtra("gpsSelected", true);
        result.putExtra("routeMapUri", routeMapUri);
        if (recordData != null) {
            result.putExtra("distance",  recordData.getDistance());
            result.putExtra("time",      recordData.getTime());
            result.putExtra("pace",      recordData.getPace());
            result.putExtra("createdAt", recordData.getCreatedAt());
        }
        requireActivity().setResult(Activity.RESULT_OK, result);
        requireActivity().finish();
    }

    private String formatTime(int s) { return String.format(Locale.KOREA, "%02d:%02d", s / 60, s % 60); }
    private String formatPaceStr(float p) { return String.format(Locale.KOREA, "%d'%02d\"", (int) p, (int) ((p - (int) p) * 60)); }
}