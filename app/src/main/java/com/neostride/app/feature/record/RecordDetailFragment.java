package com.neostride.app.feature.record;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.neostride.app.R;
import com.neostride.app.feature.running.model.GpsTraceRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordDetailFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RunningRecordResponse recordData;
    private boolean isAnalysisExpanded = false;

    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    private float paceThresVS, paceThresS, paceThresF, paceThresVF;
    private boolean thresSet = false;

    // 그래프 포인트 모델
    public static class PacePoint {
        public String timeStr;
        public float paceValue;
        public PacePoint(String timeStr, float paceValue) { this.timeStr = timeStr; this.paceValue = paceValue; }
    }

    public static RecordDetailFragment newInstance(RunningRecordResponse record) {
        RecordDetailFragment fragment = new RecordDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("record_data", record);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_detail, container, false);
        if (getArguments() != null) recordData = (RunningRecordResponse) getArguments().getSerializable("record_data");
        if (recordData != null) setupBasicUI(view);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_detail);
        if (mapFragment != null) mapFragment.getMapAsync(this);
        setupExpandableCard(view);
        return view;
    }

    private void setupBasicUI(View view) {
        TextView tvTitle = view.findViewById(R.id.tv_detail_title);
        if (recordData.getCreatedAt() != null) tvTitle.setText(recordData.getCreatedAt().split("T")[0].replace("-", "."));
        ((TextView) view.findViewById(R.id.tv_detail_distance)).setText(String.format(Locale.KOREA, "%.2f km", recordData.getDistance()));
        ((TextView) view.findViewById(R.id.tv_detail_time)).setText(formatTime((int)recordData.getTime()));
        ((TextView) view.findViewById(R.id.tv_detail_calories)).setText(String.valueOf((int) recordData.getCalories()));
        // pace < 60이면 구버전(분 단위), >= 60이면 신버전(초 단위)
        int paceSeconds = recordData.getPace() < 60 ? (int)(recordData.getPace() * 60) : (int) recordData.getPace();
        ((TextView) view.findViewById(R.id.tv_detail_pace)).setText(String.format(Locale.KOREA, "%d'%02d\"", paceSeconds / 60, paceSeconds % 60));
    }

    private void setupExpandableCard(View view) {
        View toggleArea = view.findViewById(R.id.layout_expand_toggle);
        LinearLayout expandContent = view.findViewById(R.id.layout_expand_content);
        ImageView arrowIcon = view.findViewById(R.id.iv_expand_arrow);
        ImageView chartIcon = view.findViewById(R.id.ivPaceChartGradient);
        ImageView routeCenterIcon = view.findViewById(R.id.ivRouteCenterGradient);
        FrameLayout chartContainer = view.findViewById(R.id.chart_container);

        // 인터랙션용 텍스트뷰
        LinearLayout layoutSelectedInfo = view.findViewById(R.id.layout_selected_info);
        TextView tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        TextView tvSelectedPace = view.findViewById(R.id.tv_selected_pace);

        int[] colors = {COLOR_VERY_SLOW, COLOR_SLOW, COLOR_NORMAL, COLOR_FAST, COLOR_VERY_FAST};
        float[] pos = {0f, 0.25f, 0.5f, 0.75f, 1f};
        setGradientTintToIcon(chartIcon, colors, pos);
        setGradientTintToIcon(routeCenterIcon, colors, pos);

        updatePaceThresholds();

        // [인터랙티브 줄 그래프 생성]
        if (recordData != null && recordData.getGpsPath() != null) {
            PaceLineView lineView = new PaceLineView(getContext());
            lineView.setData(calculatePacePoints(recordData.getGpsPath()));

            // 터치 콜백 설정
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

    private List<PacePoint> calculatePacePoints(List<GpsTraceRequest> path) {
        List<PacePoint> points = new ArrayList<>();
        long startMillis = parseIsoTime(path.get(0).getTime());
        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);
            double dist = distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
            long currentMillis = parseIsoTime(p2.getTime());
            long diffSec = (currentMillis - parseIsoTime(p1.getTime())) / 1000;

            if (dist > 0.001 && diffSec > 0) {
                float pace = (float) ((diffSec / 60.0) / dist);
                long elapsed = (currentMillis - startMillis) / 1000;
                String timeLabel = String.format(Locale.KOREA, "%02d:%02d", elapsed / 60, elapsed % 60);
                points.add(new PacePoint(timeLabel, Math.min(pace, 15f)));
            }
        }

        if (points.size() < 3) return points;

        // 1단계: 고립된 GPS 노이즈 스파이크만 제거
        // 인터벌 러닝 보호: 앞뒤 양쪽 이웃 모두와 크게 다를 때만 노이즈로 판정
        // → 인터벌처럼 연속된 구간 변화는 앞뒤 중 한쪽은 비슷하므로 살아남음
        for (int i = 1; i < points.size() - 1; i++) {
            float v    = points.get(i).paceValue;
            float prev = points.get(i - 1).paceValue;
            float next = points.get(i + 1).paceValue;
            // 앞뒤 모두 대비 1.8배 초과(이상하게 느림) or 0.55배 미만(이상하게 빠름) → 단독 스파이크
            boolean isSpikeHigh = v > prev * 1.8f && v > next * 1.8f;
            boolean isSpikeLow  = v < prev * 0.55f && v < next * 0.55f;
            if (isSpikeHigh || isSpikeLow) {
                points.get(i).paceValue = (prev + next) / 2f;
            }
        }

        // 2단계: 5포인트 이동평균 1회
        // 7포인트 2회 대신 가볍게 → 인터벌 경계(급격한 페이스 전환)의 형태 보존
        float[] smoothed = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            int from = Math.max(0, i - 2);
            int to   = Math.min(points.size() - 1, i + 2);
            float sum = 0;
            for (int j = from; j <= to; j++) sum += points.get(j).paceValue;
            smoothed[i] = sum / (to - from + 1);
        }
        for (int i = 0; i < points.size(); i++) points.get(i).paceValue = smoothed[i];

        return points;
    }

    // --- [고급] 커스텀 인터랙티브 선 그래프 뷰 ---
    private class PaceLineView extends View {
        private List<PacePoint> points = new ArrayList<>();
        private Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private int selectedIndex = -1;
        private OnPointSelectedListener listener;

        private float paddingLeft = 100f;
        private float paddingBottom = 60f;
        private float paddingTop = 40f;
        private float paddingRight = 40f;

        public PaceLineView(Context context) {
            super(context);
            linePaint.setStrokeWidth(6f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);

            axisPaint.setColor(Color.parseColor("#44FFFFFF"));
            axisPaint.setStrokeWidth(2f);

            textPaint.setColor(Color.parseColor("#88FFFFFF"));
            textPaint.setTextSize(24f);

            guidePaint.setColor(Color.parseColor("#CCFF00"));
            guidePaint.setStrokeWidth(3f);
            guidePaint.setPathEffect(null);
        }

        public void setData(List<PacePoint> data) { this.points = data; invalidate(); }
        public void setOnPointSelectedListener(OnPointSelectedListener l) { this.listener = l; }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (points.size() < 2) return;

            float w = getWidth() - paddingLeft - paddingRight;
            float h = getHeight() - paddingTop - paddingBottom;

            float minPace = 15f; float maxPace = 0f;
            for (PacePoint p : points) { if (p.paceValue < minPace) minPace = p.paceValue; if (p.paceValue > maxPace) maxPace = p.paceValue; }
            float range = Math.max(1f, maxPace - minPace);

            // 1. 축 그리기 (X: 시간, Y: 페이스)
            canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + h, axisPaint); // Y축
            canvas.drawLine(paddingLeft, paddingTop + h, paddingLeft + w, paddingTop + h, axisPaint); // X축

            // 2. Y축 눈금 (페이스)
            canvas.drawText(formatPaceStr(minPace), 10, paddingTop + 10, textPaint);
            canvas.drawText(formatPaceStr(maxPace), 10, paddingTop + h, textPaint);

            // 3. 선 그리기 (선분을 세분화해서 색상 변화를 부드럽게)
            float stepX = w / (points.size() - 1);
            int subDivisions = 4; // 각 선분을 4등분

            for (int i = 0; i < points.size() - 1; i++) {
                float p1 = points.get(i).paceValue;
                float p2 = points.get(i + 1).paceValue;

                float y1 = paddingTop + ((p1 - minPace) / range) * h;
                float y2 = paddingTop + ((p2 - minPace) / range) * h;

                float x1 = paddingLeft + (i * stepX);
                float x2 = paddingLeft + ((i + 1) * stepX);

                for (int s = 0; s < subDivisions; s++) {
                    float t1 = (float) s / subDivisions;
                    float t2 = (float) (s + 1) / subDivisions;

                    float sx1 = x1 + (x2 - x1) * t1;
                    float sy1 = y1 + (y2 - y1) * t1;
                    float sx2 = x1 + (x2 - x1) * t2;
                    float sy2 = y1 + (y2 - y1) * t2;

                    // 중간 페이스로 색상 결정
                    float midPace = p1 + (p2 - p1) * ((t1 + t2) / 2f);
                    linePaint.setColor(getPaceColor(midPace));
                    canvas.drawLine(sx1, sy1, sx2, sy2, linePaint);
                }
            }

            // 4. X축 눈금 (시작, 중간, 끝 시간)
            canvas.drawText(points.get(0).timeStr, paddingLeft, paddingTop + h + 40, textPaint);
            canvas.drawText(points.get(points.size()-1).timeStr, paddingLeft + w - 60, paddingTop + h + 40, textPaint);

            // 5. 선택 가이드라인 그리기
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
            float w = getWidth() - paddingLeft - paddingRight;
            float stepX = w / (points.size() - 1);

            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                float touchX = event.getX() - paddingLeft;
                selectedIndex = Math.round(touchX / stepX);
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

    // --- 색상 및 유틸 (기존 동일) ---
    private void updatePaceThresholds() {
        if (recordData == null) return;
        // pace < 60이면 구버전(분 단위), >= 60이면 신버전(초 단위) → 차트 비교용 분/km로 통일
        float avgPace = recordData.getPace() < 60 ? recordData.getPace() : recordData.getPace() / 60f;
        paceThresVS = avgPace * 1.20f; paceThresS = avgPace * 1.08f;
        paceThresF = avgPace * 0.92f; paceThresVF = avgPace * 0.80f;
        thresSet = true;
    }

    private int getPaceColor(float pace) {
        if (!thresSet) {
            if (pace >= 8.5f) return COLOR_VERY_SLOW; if (pace >= 7.5f) return COLOR_SLOW;
            if (pace >= 6.5f) return COLOR_NORMAL; if (pace >= 5.5f) return COLOR_FAST;
            return COLOR_VERY_FAST;
        }
        if (pace >= paceThresVS) return COLOR_VERY_SLOW;
        if (pace >= paceThresS) return COLOR_SLOW;
        if (pace >= paceThresF) return COLOR_NORMAL;
        if (pace >= paceThresVF) return COLOR_FAST;
        return COLOR_VERY_FAST;
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
                if (loc != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 16f));
            });
            getView().findViewById(R.id.btn_route_center).setOnClickListener(v -> {
                if (recordData != null && recordData.getGpsPath() != null && !recordData.getGpsPath().isEmpty()) {
                    LatLngBounds.Builder b = new LatLngBounds.Builder();
                    for (GpsTraceRequest p : recordData.getGpsPath()) b.include(new LatLng(p.getLatitude(), p.getLongitude()));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 300));
                }
            });
        }
        try { mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)); } catch (Exception e) { e.printStackTrace(); }
        if (recordData != null && recordData.getGpsPath() != null) drawFullRoute(recordData.getGpsPath());
    }

    private void drawFullRoute(List<GpsTraceRequest> path) {
        if (path.size() < 2) return;
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i); GpsTraceRequest p2 = path.get(i+1);
            LatLng from = new LatLng(p1.getLatitude(), p1.getLongitude()); LatLng to = new LatLng(p2.getLatitude(), p2.getLongitude());
            double d = distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
            long t = (parseIsoTime(p2.getTime()) - parseIsoTime(p1.getTime())) / 1000;
            int c = (d > 0 && t > 0) ? getPaceColor((float)((t/60.0)/d)) : COLOR_NORMAL;
            mMap.addPolyline(new PolylineOptions().add(from, to).width(14f).color(c).geodesic(true).jointType(JointType.ROUND));
            b.include(from); b.include(to);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 300));
    }

    private void setGradientTintToIcon(ImageView iv, int[] cls, float[] pts) {
        if (iv == null || iv.getDrawable() == null) return;
        Drawable d = iv.getDrawable(); Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b); d.setBounds(0, 0, c.getWidth(), c.getHeight()); d.draw(c);
        Paint p = new Paint(); p.setShader(new LinearGradient(0, 0, c.getWidth(), 0, cls, pts, Shader.TileMode.CLAMP));
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)); c.drawRect(0, 0, c.getWidth(), c.getHeight(), p);
        iv.setImageDrawable(new BitmapDrawable(getResources(), b));
    }

    private double distanceBetween(double la1, double lo1, double la2, double lo2) {
        Location l1 = new Location(""); l1.setLatitude(la1); l1.setLongitude(lo1);
        Location l2 = new Location(""); l2.setLatitude(la2); l2.setLongitude(lo2);
        return l1.distanceTo(l2) / 1000.0;
    }

    private long parseIsoTime(String t) {
        try { String c = t.replace("Z", "").replace("T", " "); if (c.contains(".")) c = c.substring(0, c.lastIndexOf("."));
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).parse(c).getTime(); } catch (Exception e) { return System.currentTimeMillis(); }
    }

    private String formatTime(int s) { return String.format(Locale.KOREA, "%02d:%02d", s / 60, s % 60); }
    private String formatPaceStr(float p) { return String.format(Locale.KOREA, "%d'%02d\"", (int)p, (int)((p-(int)p)*60)); }
}