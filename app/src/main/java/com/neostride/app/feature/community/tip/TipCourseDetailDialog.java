package com.neostride.app.feature.community.tip;

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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
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
import java.util.List;
import java.util.Locale;

/*
 * 팁 업로드 코스 선택 플로우에서 기록 상세를 보여주는 다이얼로그임
 *
 * - FeedRecordDetailDialog 와 동일한 구성이지만
 *   - 헤더 제목: "추천 러닝 코스", 버튼: "선택"
 *   - PACE ANALYSIS 카드 대신 GPS 중간 좌표 역지오코딩 주소 카드 표시
 * - "선택" 버튼 클릭 시 지도 스냅샷을 캐시에 저장하고 콜백 호출
 */
public class TipCourseDetailDialog implements OnMapReadyCallback {

    /*
     * "선택" 버튼을 눌렀을 때 호출되는 콜백임
     * address: 역지오코딩으로 얻은 코스 주소 (null 가능)
     */
    public interface OnSelectClickListener {
        void onSelectClick(RunningRecordResponse record, @Nullable String routeMapUri, @Nullable String address);
    }

    // ── 페이스 색상 상수 ──
    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW      = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL    = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST      = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    private final Context context;
    private final RunningRecordResponse recordData;
    private final OnSelectClickListener selectListener;

    private Dialog dialog;
    private MapView mapView;
    private GoogleMap mMap;

    // 역지오코딩으로 얻은 코스 중간 지점 주소 (선택 시 콜백에 포함됨)
    private String resolvedAddress = "";

    private View btnMyLocation;
    private View btnRouteCenter;

    // ── 페이스 임계값 ──
    private float paceThresVS, paceThresS, paceThresF, paceThresVF;
    private boolean thresSet = false;

    public TipCourseDetailDialog(@NonNull Context context,
                                 @NonNull RunningRecordResponse record,
                                 @NonNull OnSelectClickListener listener) {
        this.context        = context;
        this.recordData     = record;
        this.selectListener = listener;
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tip_course_detail);

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

        ImageView btnBack  = dialog.findViewById(R.id.btn_back_record_detail);
        TextView btnSelect = dialog.findViewById(R.id.btn_upload_record_detail);
        mapView            = dialog.findViewById(R.id.map_detail_view);
        btnMyLocation      = dialog.findViewById(R.id.btn_my_location);
        btnRouteCenter     = dialog.findViewById(R.id.btn_route_center);

        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnSelect.setOnClickListener(v -> {
            v.setEnabled(false);
            captureMapAndSelect();
        });

        setupBasicUI();
        updatePaceThresholds();
        fetchMidpointAddress();

        // 경로 중심 버튼 아이콘에 무지개 그라데이션 적용
        int[]   colors = {COLOR_VERY_SLOW, COLOR_SLOW, COLOR_NORMAL, COLOR_FAST, COLOR_VERY_FAST};
        float[] pos    = {0f, 0.25f, 0.5f, 0.75f, 1f};
        ImageView routeCenterIcon = dialog.findViewById(R.id.ivRouteCenterGradient);
        setGradientTintToIcon(routeCenterIcon, colors, pos);

        mapView.onCreate(null);
        dialog.setOnDismissListener(d -> {
            mapView.onPause();
            mapView.onDestroy();
        });

        dialog.show();
        mapView.onResume();
        mapView.getMapAsync(this);
    }

    // ── 기본 통계(날짜·거리·시간·칼로리·페이스) 바인딩 ──
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

    /*
     * GPS 경로 중간 좌표를 계산해 역지오코딩으로 주소를 가져와 tv_course_address에 표시함
     * Geocoder 는 IO 블로킹이므로 백그라운드 스레드에서 실행 후 메인 스레드에 결과 전달함
     */
    private void fetchMidpointAddress() {
        List<GpsTraceRequest> path = recordData.getGpsPath();
        if (path == null || path.isEmpty()) {
            setAddressText("위치 정보 없음");
            return;
        }

        // 중간 인덱스 좌표
        GpsTraceRequest midPoint = path.get(path.size() / 2);
        double lat = midPoint.getLatitude();
        double lng = midPoint.getLongitude();

        new Thread(() -> {
            String address = reverseGeocode(lat, lng);
            new Handler(Looper.getMainLooper()).post(() -> setAddressText(address));
        }).start();
    }

    private void setAddressText(String address) {
        resolvedAddress = address != null ? address : "";
        if (dialog == null || !dialog.isShowing()) return;
        TextView tvAddress = dialog.findViewById(R.id.tv_course_address);
        if (tvAddress != null) tvAddress.setText(resolvedAddress);
    }

    /*
     * 위도·경도로 도로명 주소를 반환함
     * Geocoder 사용 불가 또는 결과 없을 때 위도·경도 좌표 문자열로 대체함
     */
    private String reverseGeocode(double lat, double lng) {
        try {
            if (!Geocoder.isPresent()) {
                return String.format(Locale.KOREA, "%.5f, %.5f", lat, lng);
            }
            Geocoder geocoder = new Geocoder(context, Locale.KOREA);
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                // 도로명 주소 우선, 없으면 행정구역 조합
                String line = addr.getAddressLine(0);
                if (line != null && !line.isEmpty()) {
                    // "대한민국 " 접두어 제거
                    return line.replace("대한민국 ", "").trim();
                }
                // fallback: 행정구역 수동 조합
                StringBuilder sb = new StringBuilder();
                if (addr.getAdminArea()    != null) sb.append(addr.getAdminArea()).append(" ");
                if (addr.getSubAdminArea() != null) sb.append(addr.getSubAdminArea()).append(" ");
                if (addr.getLocality()     != null) sb.append(addr.getLocality()).append(" ");
                if (addr.getThoroughfare() != null) sb.append(addr.getThoroughfare());
                return sb.toString().trim();
            }
        } catch (Exception ignored) {}
        return String.format(Locale.KOREA, "%.5f, %.5f", lat, lng);
    }

    // ── OnMapReadyCallback ──
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

    // ── 지도 스냅샷 캡처 후 선택 리스너 호출 ──
    private void captureMapAndSelect() {
        if (mMap == null) {
            selectListener.onSelectClick(recordData, null, resolvedAddress);
            dialog.dismiss();
            return;
        }
        moveCameraToRoute();
        mMap.snapshot(bitmap -> {
            String uri = saveBitmapToCache(bitmap);
            selectListener.onSelectClick(recordData, uri, resolvedAddress);
            dialog.dismiss();
        });
    }

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
            long time = (parseIsoTime(p2.getTime()) - parseIsoTime(p1.getTime())) / 1000;
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
            return null;
        }
    }

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

    // ── ImageView 에 수평 그라데이션 색상 합성 ──
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

    private double distanceBetween(double la1, double lo1, double la2, double lo2) {
        android.location.Location l1 = new android.location.Location("");
        l1.setLatitude(la1); l1.setLongitude(lo1);
        android.location.Location l2 = new android.location.Location("");
        l2.setLatitude(la2); l2.setLongitude(lo2);
        return l1.distanceTo(l2) / 1000.0;
    }

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
}
