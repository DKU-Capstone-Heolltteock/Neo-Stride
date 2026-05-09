package com.neostride.app.feature.record;

import android.annotation.SuppressLint;
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
import com.neostride.app.common.network.MockFeedStorage;
import com.neostride.app.feature.feed.FeedUploadDialog;
import com.neostride.app.feature.running.model.GpsTraceRequest;
import com.neostride.app.feature.running.model.RunningRecordResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 기록 상세 화면을 담당하는 Fragment임
public class RecordDetailFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RunningRecordResponse recordData;

    private ActivityResultLauncher<String[]> feedPhotoPickerLauncher;
    private FeedUploadDialog feedUploadDialog;

    private boolean isAnalysisExpanded = false;

    private static final int COLOR_VERY_SLOW = Color.parseColor("#FF3B30");
    private static final int COLOR_SLOW = Color.parseColor("#FF9500");
    private static final int COLOR_NORMAL = Color.parseColor("#FFCC00");
    private static final int COLOR_FAST = Color.parseColor("#A8D600");
    private static final int COLOR_VERY_FAST = Color.parseColor("#34C759");

    private float paceThresVS;
    private float paceThresS;
    private float paceThresF;
    private float paceThresVF;
    private boolean thresSet = false;

    public static class PacePoint {
        public String timeStr;
        public float paceValue;

        public PacePoint(String timeStr, float paceValue) {
            this.timeStr = timeStr;
            this.paceValue = paceValue;
        }
    }

    public static RecordDetailFragment newInstance(RunningRecordResponse record) {
        RecordDetailFragment fragment = new RecordDetailFragment();

        Bundle args = new Bundle();
        args.putSerializable("record_data", record);
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
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_record_detail, container, false);

        if (getArguments() != null) {
            recordData = (RunningRecordResponse) getArguments().getSerializable("record_data");
        }

        if (recordData != null) {
            setupBasicUI(view);
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(R.id.map_detail);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupExpandableCard(view);

        return view;
    }

    private void setupBasicUI(View view) {
        TextView tvTitle = view.findViewById(R.id.tv_detail_title);

        if (recordData.getCreatedAt() != null) {
            tvTitle.setText(
                    recordData.getCreatedAt()
                            .split("T")[0]
                            .replace("-", ".")
            );
        }

        ((TextView) view.findViewById(R.id.tv_detail_distance))
                .setText(String.format(Locale.KOREA, "%.2f km", recordData.getDistance()));

        ((TextView) view.findViewById(R.id.tv_detail_time))
                .setText(formatTime((int) recordData.getTime()));

        ((TextView) view.findViewById(R.id.tv_detail_calories))
                .setText(String.valueOf((int) recordData.getCalories()));

        double paceVal = recordData.getPace();

        ((TextView) view.findViewById(R.id.tv_detail_pace))
                .setText(String.format(
                        Locale.KOREA,
                        "%d'%02d\"",
                        (int) paceVal,
                        (int) ((paceVal - (int) paceVal) * 60)
                ));
    }

    private void setupExpandableCard(View view) {
        View toggleArea = view.findViewById(R.id.layout_expand_toggle);
        LinearLayout expandContent = view.findViewById(R.id.layout_expand_content);
        ImageView arrowIcon = view.findViewById(R.id.iv_expand_arrow);
        ImageView chartIcon = view.findViewById(R.id.ivPaceChartGradient);
        ImageView routeCenterIcon = view.findViewById(R.id.ivRouteCenterGradient);
        FrameLayout chartContainer = view.findViewById(R.id.chart_container);

        LinearLayout layoutSelectedInfo = view.findViewById(R.id.layout_selected_info);
        TextView tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        TextView tvSelectedPace = view.findViewById(R.id.tv_selected_pace);

        int[] colors = {
                COLOR_VERY_SLOW,
                COLOR_SLOW,
                COLOR_NORMAL,
                COLOR_FAST,
                COLOR_VERY_FAST
        };

        float[] pos = {
                0f,
                0.25f,
                0.5f,
                0.75f,
                1f
        };

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

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (getView() != null) {
            getView().findViewById(R.id.btn_my_location).setOnClickListener(v -> {
                @SuppressLint("MissingPermission")
                Location loc = mMap.getMyLocation();

                if (loc != null) {
                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(
                                            loc.getLatitude(),
                                            loc.getLongitude()
                                    ),
                                    16f
                            )
                    );
                }
            });

            getView().findViewById(R.id.btn_route_center).setOnClickListener(v -> {
                moveCameraToRoute();
            });

            getView().findViewById(R.id.btn_share_circle).setOnClickListener(v -> {
                captureMapAndOpenFeedDialog();
            });
        }

        try {
            mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            requireContext(),
                            R.raw.map_style
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (recordData != null && recordData.getGpsPath() != null) {
            drawFullRoute(recordData.getGpsPath());
        }
    }

    private void captureMapAndOpenFeedDialog() {
        if (mMap == null) {
            openFeedDialog(null);
            return;
        }

        moveCameraToRoute();

        mMap.snapshot(bitmap -> {
            String routeMapUri = saveBitmapToCache(bitmap);
            openFeedDialog(routeMapUri);
        });
    }

    private String saveBitmapToCache(Bitmap bitmap) {
        if (bitmap == null) return null;

        try {
            File file = new File(
                    requireContext().getCacheDir(),
                    "route_map_" + System.currentTimeMillis() + ".png"
            );

            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return Uri.fromFile(file).toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openFeedDialog(String routeMapUri) {
        feedUploadDialog = new FeedUploadDialog(
                requireContext(),
                recordData,
                routeMapUri,
                () -> feedPhotoPickerLauncher.launch(new String[]{"image/*"}),
                response -> {
                    //MockFeedStorage.addFeedFromResponse(response);

                    Toast.makeText(
                            requireContext(),
                            "피드 생성 완료: " + response.getTitle(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        feedUploadDialog.show();
    }

    private void moveCameraToRoute() {
        if (mMap == null
                || recordData == null
                || recordData.getGpsPath() == null
                || recordData.getGpsPath().isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (GpsTraceRequest point : recordData.getGpsPath()) {
            builder.include(
                    new LatLng(
                            point.getLatitude(),
                            point.getLongitude()
                    )
            );
        }

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                        builder.build(),
                        300
                )
        );
    }

    private void drawFullRoute(List<GpsTraceRequest> path) {
        if (path.size() < 2) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);

            LatLng from = new LatLng(
                    p1.getLatitude(),
                    p1.getLongitude()
            );

            LatLng to = new LatLng(
                    p2.getLatitude(),
                    p2.getLongitude()
            );

            double distance = distanceBetween(
                    p1.getLatitude(),
                    p1.getLongitude(),
                    p2.getLatitude(),
                    p2.getLongitude()
            );

            long time = (
                    parseIsoTime(p2.getTime()) -
                            parseIsoTime(p1.getTime())
            ) / 1000;

            int color = COLOR_NORMAL;

            if (distance > 0 && time > 0) {
                float pace = (float) ((time / 60.0) / distance);
                color = getPaceColor(pace);
            }

            mMap.addPolyline(
                    new PolylineOptions()
                            .add(from, to)
                            .width(14f)
                            .color(color)
                            .geodesic(true)
                            .jointType(JointType.ROUND)
            );

            builder.include(from);
            builder.include(to);
        }

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                        builder.build(),
                        300
                )
        );
    }

    private List<PacePoint> calculatePacePoints(List<GpsTraceRequest> path) {
        List<PacePoint> points = new ArrayList<>();

        if (path == null || path.size() < 2) {
            return points;
        }

        long startMillis = parseIsoTime(path.get(0).getTime());

        for (int i = 0; i < path.size() - 1; i++) {
            GpsTraceRequest p1 = path.get(i);
            GpsTraceRequest p2 = path.get(i + 1);

            double distance = distanceBetween(
                    p1.getLatitude(),
                    p1.getLongitude(),
                    p2.getLatitude(),
                    p2.getLongitude()
            );

            long currentMillis = parseIsoTime(p2.getTime());
            long diffSec = (
                    currentMillis -
                            parseIsoTime(p1.getTime())
            ) / 1000;

            if (distance > 0.001 && diffSec > 0) {
                float pace = (float) ((diffSec / 60.0) / distance);

                long elapsed = (currentMillis - startMillis) / 1000;

                String timeLabel = String.format(
                        Locale.KOREA,
                        "%02d:%02d",
                        elapsed / 60,
                        elapsed % 60
                );

                points.add(
                        new PacePoint(
                                timeLabel,
                                Math.min(pace, 15f)
                        )
                );
            }
        }

        return points;
    }

    private class PaceLineView extends View {

        private List<PacePoint> points = new ArrayList<>();

        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private int selectedIndex = -1;
        private OnPointSelectedListener listener;

        private final float paddingLeft = 100f;
        private final float paddingBottom = 60f;
        private final float paddingTop = 40f;
        private final float paddingRight = 40f;

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
        }

        public void setData(List<PacePoint> data) {
            this.points = data;
            invalidate();
        }

        public void setOnPointSelectedListener(OnPointSelectedListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (points.size() < 2) return;

            float width = getWidth() - paddingLeft - paddingRight;
            float height = getHeight() - paddingTop - paddingBottom;

            float minPace = 15f;
            float maxPace = 0f;

            for (PacePoint point : points) {
                if (point.paceValue < minPace) minPace = point.paceValue;
                if (point.paceValue > maxPace) maxPace = point.paceValue;
            }

            float range = Math.max(1f, maxPace - minPace);

            canvas.drawLine(
                    paddingLeft,
                    paddingTop,
                    paddingLeft,
                    paddingTop + height,
                    axisPaint
            );

            canvas.drawLine(
                    paddingLeft,
                    paddingTop + height,
                    paddingLeft + width,
                    paddingTop + height,
                    axisPaint
            );

            canvas.drawText(
                    formatPaceStr(minPace),
                    10,
                    paddingTop + 10,
                    textPaint
            );

            canvas.drawText(
                    formatPaceStr(maxPace),
                    10,
                    paddingTop + height,
                    textPaint
            );

            float stepX = width / (points.size() - 1);
            int subDivisions = 4;

            for (int i = 0; i < points.size() - 1; i++) {
                float p1 = points.get(i).paceValue;
                float p2 = points.get(i + 1).paceValue;

                float y1 = paddingTop + ((p1 - minPace) / range) * height;
                float y2 = paddingTop + ((p2 - minPace) / range) * height;

                float x1 = paddingLeft + (i * stepX);
                float x2 = paddingLeft + ((i + 1) * stepX);

                for (int s = 0; s < subDivisions; s++) {
                    float t1 = (float) s / subDivisions;
                    float t2 = (float) (s + 1) / subDivisions;

                    float sx1 = x1 + (x2 - x1) * t1;
                    float sy1 = y1 + (y2 - y1) * t1;
                    float sx2 = x1 + (x2 - x1) * t2;
                    float sy2 = y1 + (y2 - y1) * t2;

                    float midPace = p1 + (p2 - p1) * ((t1 + t2) / 2f);

                    linePaint.setColor(getPaceColor(midPace));

                    canvas.drawLine(
                            sx1,
                            sy1,
                            sx2,
                            sy2,
                            linePaint
                    );
                }
            }

            canvas.drawText(
                    points.get(0).timeStr,
                    paddingLeft,
                    paddingTop + height + 40,
                    textPaint
            );

            canvas.drawText(
                    points.get(points.size() - 1).timeStr,
                    paddingLeft + width - 60,
                    paddingTop + height + 40,
                    textPaint
            );

            if (selectedIndex != -1) {
                float selectedX = paddingLeft + (selectedIndex * stepX);

                canvas.drawLine(
                        selectedX,
                        paddingTop,
                        selectedX,
                        paddingTop + height,
                        guidePaint
                );

                float selectedY =
                        paddingTop +
                                ((points.get(selectedIndex).paceValue - minPace) / range) * height;

                canvas.drawCircle(
                        selectedX,
                        selectedY,
                        10f,
                        guidePaint
                );
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (points.isEmpty()) return false;

            float width = getWidth() - paddingLeft - paddingRight;
            float stepX = width / (points.size() - 1);

            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {

                float touchX = event.getX() - paddingLeft;

                selectedIndex = Math.round(touchX / stepX);

                if (selectedIndex < 0) selectedIndex = 0;
                if (selectedIndex >= points.size()) selectedIndex = points.size() - 1;

                if (listener != null) {
                    listener.onPointSelected(points.get(selectedIndex));
                }

                invalidate();
                return true;
            }

            return super.onTouchEvent(event);
        }
    }

    public interface OnPointSelectedListener {
        void onPointSelected(PacePoint point);
    }

    private void updatePaceThresholds() {
        if (recordData == null) return;

        float avgPace = (float) recordData.getPace();

        paceThresVS = avgPace * 1.20f;
        paceThresS = avgPace * 1.08f;
        paceThresF = avgPace * 0.92f;
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
        if (pace >= paceThresS) return COLOR_SLOW;
        if (pace >= paceThresF) return COLOR_NORMAL;
        if (pace >= paceThresVF) return COLOR_FAST;

        return COLOR_VERY_FAST;
    }

    private void setGradientTintToIcon(ImageView imageView, int[] colors, float[] points) {
        if (imageView == null || imageView.getDrawable() == null) return;

        Drawable drawable = imageView.getDrawable();

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );

        drawable.draw(canvas);

        Paint paint = new Paint();

        paint.setShader(
                new LinearGradient(
                        0,
                        0,
                        canvas.getWidth(),
                        0,
                        colors,
                        points,
                        Shader.TileMode.CLAMP
                )
        );

        paint.setXfermode(
                new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        );

        canvas.drawRect(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight(),
                paint
        );

        imageView.setImageDrawable(
                new BitmapDrawable(getResources(), bitmap)
        );
    }

    private double distanceBetween(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        Location location1 = new Location("");
        location1.setLatitude(lat1);
        location1.setLongitude(lon1);

        Location location2 = new Location("");
        location2.setLatitude(lat2);
        location2.setLongitude(lon2);

        return location1.distanceTo(location2) / 1000.0;
    }

    private long parseIsoTime(String time) {
        try {
            String cleanTime = time.replace("Z", "").replace("T", " ");

            if (cleanTime.contains(".")) {
                cleanTime = cleanTime.substring(
                        0,
                        cleanTime.lastIndexOf(".")
                );
            }

            return new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.KOREA
            ).parse(cleanTime).getTime();

        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private String formatTime(int seconds) {
        return String.format(
                Locale.KOREA,
                "%02d:%02d",
                seconds / 60,
                seconds % 60
        );
    }

    private String formatPaceStr(float pace) {
        return String.format(
                Locale.KOREA,
                "%d'%02d\"",
                (int) pace,
                (int) ((pace - (int) pace) * 60)
        );
    }
}