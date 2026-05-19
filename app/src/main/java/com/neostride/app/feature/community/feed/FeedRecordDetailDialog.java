package com.neostride.app.feature.community.feed;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.neostride.app.R;
import com.neostride.app.feature.main.running.model.GpsTraceRequest;
import com.neostride.app.feature.main.running.model.RunningRecordResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * 피드 업로드 플로우에서 기록 상세를 보여주는 다이얼로그임
 *
 * - 기록 선택 화면(FeedRecordPickerDialog)에서 기록 카드를 누르면 이 다이얼로그가 열린다.
 * - 기록탭의 RecordDetailFragment 와 동일하게 지도·통계·페이스 분석을 표시한다.
 * - 기록탭의 업로드(공유) 아이콘 대신 상단 "업로드" 텍스트 버튼으로 FeedUploadDialog 를 연다.
 * - 뒤로가기(< 버튼)를 누르면 이 다이얼로그만 닫히고 기록 선택 화면으로 돌아간다.
 */
public class FeedRecordDetailDialog implements OnMapReadyCallback {

    /*
     * "업로드" 버튼을 눌렀을 때 호출되는 콜백임
     * routeMapUri: 지도 스냅샷을 캐시에 저장한 URI (null 일 수도 있음)
     */
    public interface OnUploadClickListener {
        void onUploadClick(RunningRecordResponse record, @Nullable String routeMapUri);
    }

    // ── 페이스 색상 상수 (RecordDetailFragment 와 동일) ──
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    private final Context context;
    private final RunningRecordResponse recordData;
    private final OnUploadClickListener uploadListener;

    private Dialog dialog;
    private MapView mapView;
    private GoogleMap mMap;

    private boolean isAnalysisExpanded = false;

    // ── 페이스 임계값 ──
    private float paceThresVS, paceThresS, paceThresF, paceThresVF;
    private boolean thresSet = false;

    // ── 지도 컨트롤 버튼 참조 (onMapReady에서 사용) ──
    private View btnMyLocation;
    private View btnRouteCenter;

    public FeedRecordDetailDialog(@NonNull Context context,
                                  @NonNull RunningRecordResponse record,
                                  @NonNull OnUploadClickListener listener) {
        this.context        = context;
        this.recordData     = record;
        this.uploadListener = listener;
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_feed_record_detail);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            int sw = context.getResources().getDisplayMetrics().widthPixels;
            int sh = context.getResources().getDisplayMetrics().heightPixels;
            params.width  = (int) (sw * 0.92f);
            params.height = (int) (sh * 0.85f);
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }

        // ── 뷰 바인딩 ───────────────────────────────────────────────────────
        ImageView btnBack  = dialog.findViewById(R.id.btn_back_record_detail);
        TextView btnUpload = dialog.findViewById(R.id.btn_upload_record_detail);
        mapView            = dialog.findViewById(R.id.map_detail_view);
        btnMyLocation      = dialog.findViewById(R.id.btn_my_location);
        btnRouteCenter     = dialog.findViewById(R.id.btn_route_center);

        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnUpload.setOnClickListener(v -> captureMapAndUpload());

        setupBasicUI();
        updatePaceThresholds();
        setupExpandableCard();

        // ── MapView 수명주기 초기화 ──────────────────────────────────────────
        mapView.onCreate(null);
        dialog.setOnDismissListener(d -> {
            mapView.onPause();
            mapView.onDestroy();
        });

        dialog.show();
        mapView.onResume();
        mapView.getMapAsync(this);
    }

    // ── 기본 통계(날짜·거리·시간·칼로리·페이스) 바인딩 ──────────────────────
    private void setupBasicUI() {
        TextView tvTitle = dialog.findViewById(R.id.tv_detail_title);
        if (recordData.getCreatedAt() != null) {
            tvTitle.setText(recordData.getCreatedAt().split("T")[0].replace("-", "."));
        }
        ((TextView) dialog.findViewById(R.id.tv_detail_distance))
                .setText(String.format(Locale.KOREA, "%.2f km", recordData.getDistance()));
        ((TextView) dialog.findViewById(R.id.tv_detail_time))
                .setText(formatTime((int) recordData.getTime()));
        ((TextView) dialog.findViewById(R.id.tv_detail_calories))
                .setText(String.valueOf((int) recordData.getCalories()));

        int paceSeconds = recordData.getPace() < 60
                ? (int) (recordData.getPace() * 60)
                : (int) recordData.getPace();
        ((TextView) dialog.findViewById(R.id.tv_detail_pace))
                .setText(String.format(Locale.KOREA, "%d'%02d\"", paceSeconds / 60, paceSeconds % 60));
    }

    // ── 페이스 분석 카드 토글 및 PaceLineView 주입 ──────────────────────────
    private void setupExpandableCard() {
        View toggleArea            = dialog.findViewById(R.id.layout_expand_toggle);
        LinearLayout expandContent = dialog.findViewById(R.id.layout_expand_content);
        ImageView arrowIcon        = dialog.findViewById(R.id.iv_expand_arrow);
        ImageView chartIcon        = dialog.findViewById(R.id.ivPaceChartGradient);
        ImageView routeCenterIcon  = dialog.findViewById(R.id.ivRouteCenterGradient);
        FrameLayout chartContainer = dialog.findViewById(R.id.chart_container);
        LinearLayout layoutSelectedInfo = dialog.findViewById(R.id.layout_selected_info);
        TextView tvSelectedTime    = dialog.findViewById(R.id.tv_selected_time);
        TextView tvSelectedPace    = dialog.findViewById(R.id.tv_selected_pace);

        int[]   colors = {COLOR_VERY_SLOW, COLOR_SLOW, COLOR_NORMAL, COLOR_FAST, COLOR_VERY_FAST};
        float[] pos    = {0f, 0.25f, 0.5f, 0.75f, 1f};
        setGradientTintToIcon(chartIcon, colors, pos);
        setGradientTintToIcon(routeCenterIcon, colors, pos);

        if (recordData.getGpsPath() != null) {
            PaceLineView lineView = new PaceLineView(context);
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

    // ── OnMapReadyCallback ────────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(35.9, 127.7), 7.0f));
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        try { mMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}

        btnMyLocation.setOnClickListener(v -> {
            @SuppressLint("MissingPermission") Location loc = mMap.getMyLocation();
            if (loc != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(loc.getLatitude(), loc.getLongitude()), 16f));
            }
        });
        btnRouteCenter.setOnClickListener(v -> moveCameraToRoute());

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
        } catch (Exception ignored) {}

        if (recordData.getGpsPath() != null) {
            drawFullRoute(recordData.getGpsPath());
        }
    }

    // ── 지도 스냅샷 캡처 후 업로드 리스너 호출 ────────────────────────────
    private void captureMapAndUpload() {
        if (mMap == null) {
            uploadListener.onUploadClick(recordData, null);
            dialog.dismiss();
            return;
        }
        moveCameraToRoute();
        mMap.snapshot(bitmap -> {
            String uri = saveBitmapToCache(bitmap);
            uploadListener.onUploadClick(recordData, uri);
            dialog.dismiss();
        });
    }

    // ── GPS 경로 전체가 보이도록 카메라 이동 ──────────────────────────────
    private void moveCameraToRoute() {
        if (mMap == null || recordData.getGpsPath() == null
                || recordData.getGpsPath().isEmpty()) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (GpsTraceRequest p : recordData.getGpsPath()) {
            builder.include(new LatLng(p.getLatitude(), p.getLongitude()));
        }
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
        } catch (Exception ignored) {}
    }

    // ── 페이스별 색상 폴리라인을 지도에 그림 ─────────────────────────────
    private void drawFullRoute(List<GpsTraceRequest> path) {
        if (path.size() < 2) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);
            LatLng from = new LatLng(p1.getLatitude(), p1.getLongitude());
            LatLng to   = new LatLng(p2.getLatitude(), p2.getLongitude());
            double dist = distanceBetween(p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());
            long   time = (parseIsoTime(p2.getTime()) - parseIsoTime(p1.getTime())) / 1000;
            int color = COLOR_NORMAL;
            if (dist > 0 && time > 0) color = getPaceColor((float) ((time / 60.0) / dist));
            mMap.addPolyline(new PolylineOptions()
                    .add(from, to).width(14f).color(color)
                    .geodesic(true).jointType(JointType.ROUND));
            builder.include(from);
            builder.include(to);
        }
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
        } catch (Exception ignored) {}
    }

    // ── Bitmap → 캐시 디렉터리에 저장 후 URI 반환 ─────────────────────
    @Nullable
    private String saveBitmapToCache(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            File file = new File(context.getCacheDir(),
                    "route_map_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── GPS 경로에서 구간별 페이스 계산 후 PacePoint 목록 반환 ──────────
    //    1단계: 스파이크 노이즈 제거  2단계: 7포인트 이동평균
    private List<PacePoint> calculatePacePoints(List<GpsTraceRequest> path) {
        List<PacePoint> points = new ArrayList<>();
        if (path == null || path.size() < 2) return points;

        long startMillis = parseIsoTime(path.get(0).getTime());
        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);
            double dist   = distanceBetween(p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());
            long curMs    = parseIsoTime(p2.getTime());
            long diffSec  = (curMs - parseIsoTime(p1.getTime())) / 1000;
            if (dist > 0.001 && diffSec > 0) {
                float pace   = (float) ((diffSec / 60.0) / dist);
                long elapsed = (curMs - startMillis) / 1000;
                String label = String.format(Locale.KOREA, "%02d:%02d", elapsed / 60, elapsed % 60);
                points.add(new PacePoint(label, Math.min(pace, 15f)));
            }
        }
        if (points.size() < 3) return points;

        // 스파이크 제거
        for (int i = 1; i < points.size() - 1; i++) {
            float v = points.get(i).paceValue;
            float prev = points.get(i - 1).paceValue;
            float next = points.get(i + 1).paceValue;
            if ((v > prev * 1.5f && v > next * 1.5f) || (v < prev * 0.65f && v < next * 0.65f)) {
                points.get(i).paceValue = (prev + next) / 2f;
            }
        }
        // 이동평균
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

    // ── 평균 페이스 기반 5단계 색상 임계값 계산 ──────────────────────────
    private void updatePaceThresholds() {
        if (recordData == null) return;
        float avgPace = recordData.getPace() < 60
                ? recordData.getPace() : recordData.getPace() / 60f;
        paceThresVS = avgPace * 1.20f;
        paceThresS  = avgPace * 1.08f;
        paceThresF  = avgPace * 0.92f;
        paceThresVF = avgPace * 0.80f;
        thresSet = true;
    }

    private int getPaceColor(float pace) {
        if (!thresSet) {
            if (pace >= 8.5f) return COLOR_VERY_SLOW;
            if (pace >= 7.5f) return COLOR_SLOW;
            if (pace >= 6.5f) return COLOR_NORMAL;
            if (pace >= 5.5f) return COLOR_FAST;
            return COLOR_VERY_FAST;
        }
        if (pace >= paceThresVS) return COLOR_VERY_SLOW;
        if (pace >= paceThresS)  return COLOR_SLOW;
        if (pace >= paceThresF)  return COLOR_NORMAL;
        if (pace >= paceThresVF) return COLOR_FAST;
        return COLOR_VERY_FAST;
    }

    // ── ImageView 에 수평 그라데이션 색상 합성 ─────────────────────────
    private void setGradientTintToIcon(ImageView iv, int[] cls, float[] pts) {
        if (iv == null || iv.getDrawable() == null) return;
        Drawable d = iv.getDrawable();
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        Paint p = new Paint();
        p.setShader(new LinearGradient(0, 0, c.getWidth(), 0, cls, pts, Shader.TileMode.CLAMP));
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), p);
        iv.setImageDrawable(new BitmapDrawable(context.getResources(), b));
    }

    // ── 두 좌표 사이의 거리(km) ──────────────────────────────────────────
    private double distanceBetween(double la1, double lo1, double la2, double lo2) {
        android.location.Location l1 = new android.location.Location("");
        l1.setLatitude(la1); l1.setLongitude(lo1);
        android.location.Location l2 = new android.location.Location("");
        l2.setLatitude(la2); l2.setLongitude(lo2);
        return l1.distanceTo(l2) / 1000.0;
    }

    // ── ISO-8601 타임스탬프 → 밀리초 ──────────────────────────────────────
    private long parseIsoTime(String t) {
        try {
            String c = t.replace("Z", "").replace("T", " ");
            if (c.contains(".")) c = c.substring(0, c.lastIndexOf("."));
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).parse(c).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private String formatTime(int s) {
        return String.format(Locale.KOREA, "%02d:%02d", s / 60, s % 60);
    }

    private String formatPaceStr(float p) {
        return String.format(Locale.KOREA, "%d'%02d\"", (int) p, (int) ((p - (int) p) * 60));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PacePoint — 페이스 차트의 단일 데이터 포인트 (시간 라벨 + 페이스 값)
    // ════════════════════════════════════════════════════════════════════════
    public static class PacePoint {
        public String timeStr;
        public float paceValue;
        PacePoint(String timeStr, float paceValue) {
            this.timeStr   = timeStr;
            this.paceValue = paceValue;
        }
    }

    // PaceLineView 에서 사용하는 콜백 인터페이스 — 비-static 이너 클래스 안에는
    // interface 를 선언할 수 없으므로 바깥(클래스 레벨)에 정의한다.
    interface OnPointSelectedListener {
        void onPointSelected(PacePoint point);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PaceLineView — 시간별 페이스 변화를 색상 꺾은선으로 그리는 커스텀 View
    // ════════════════════════════════════════════════════════════════════════
    private class PaceLineView extends View {
        private List<PacePoint> points = new ArrayList<>();
        private final Paint linePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int selectedIndex = -1;
        private OnPointSelectedListener listener;
        private final float PL = 100f, PB = 60f, PT = 40f, PR = 40f;

        PaceLineView(Context ctx) {
            super(ctx);
            linePaint.setStrokeWidth(6f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            axisPaint.setColor(Color.parseColor("#44FFFFFF"));
            axisPaint.setStrokeWidth(2f);
            textPaint.setColor(Color.parseColor("#88FFFFFF"));
            textPaint.setTextSize(24f);
            guidePaint.setColor(Color.parseColor("#CCFF00"));
            guidePaint.setStrokeWidth(3f);
        }

        void setData(List<PacePoint> data) { this.points = data; invalidate(); }
        void setOnPointSelectedListener(OnPointSelectedListener l) { this.listener = l; }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (points.size() < 2) return;

            float w = getWidth() - PL - PR;
            float h = getHeight() - PT - PB;
            float minPace = 15f, maxPace = 0f;
            for (PacePoint p : points) {
                if (p.paceValue < minPace) minPace = p.paceValue;
                if (p.paceValue > maxPace) maxPace = p.paceValue;
            }
            float range = Math.max(1f, maxPace - minPace);

            canvas.drawLine(PL, PT, PL, PT + h, axisPaint);
            canvas.drawLine(PL, PT + h, PL + w, PT + h, axisPaint);
            canvas.drawText(formatPaceStr(minPace), 10, PT + 10, textPaint);
            canvas.drawText(formatPaceStr(maxPace), 10, PT + h, textPaint);

            float stepX = w / (points.size() - 1);
            for (int i = 0; i < points.size() - 1; i++) {
                float p1 = points.get(i).paceValue, p2 = points.get(i + 1).paceValue;
                float y1 = PT + ((p1 - minPace) / range) * h;
                float y2 = PT + ((p2 - minPace) / range) * h;
                float x1 = PL + (i * stepX), x2 = PL + ((i + 1) * stepX);
                for (int s = 0; s < 4; s++) {
                    float t1 = s / 4f, t2 = (s + 1) / 4f;
                    linePaint.setColor(getPaceColor(p1 + (p2 - p1) * ((t1 + t2) / 2f)));
                    canvas.drawLine(x1 + (x2 - x1) * t1, y1 + (y2 - y1) * t1,
                            x1 + (x2 - x1) * t2, y1 + (y2 - y1) * t2, linePaint);
                }
            }
            canvas.drawText(points.get(0).timeStr, PL, PT + h + 40, textPaint);
            canvas.drawText(points.get(points.size() - 1).timeStr,
                    PL + w - 60, PT + h + 40, textPaint);

            if (selectedIndex != -1) {
                float selX = PL + (selectedIndex * stepX);
                canvas.drawLine(selX, PT, selX, PT + h, guidePaint);
                float selY = PT + ((points.get(selectedIndex).paceValue - minPace) / range) * h;
                canvas.drawCircle(selX, selY, 10f, guidePaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (points.isEmpty()) return false;
            float stepX = (getWidth() - PL - PR) / (points.size() - 1);
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {
                selectedIndex = Math.round((event.getX() - PL) / stepX);
                if (selectedIndex < 0) selectedIndex = 0;
                if (selectedIndex >= points.size()) selectedIndex = points.size() - 1;
                if (listener != null) listener.onPointSelected(points.get(selectedIndex));
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
