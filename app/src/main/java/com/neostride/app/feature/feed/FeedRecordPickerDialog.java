package com.neostride.app.feature.feed;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.neostride.app.R;
import com.neostride.app.common.network.TokenManager;
import com.neostride.app.feature.record.CalendarAdapter;
import com.neostride.app.feature.record.CalendarDayItem;
import com.neostride.app.feature.record.DailyRecordAdapter;
import com.neostride.app.feature.record.RunningRecordItem;
import com.neostride.app.feature.running.model.RunningRecordResponse;
import com.neostride.app.feature.running.repository.RunningRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * 피드 작성 시 연결할 러닝 기록을 달력에서 선택하는 다이얼로그임
 * - ViewPager2로 월별 달력을 표시하고 좌우 스와이프로 월을 이동할 수 있음
 * - 각 페이지 안에 달력 + 날짜별 기록 영역이 함께 있어 전체 영역에서 스와이프 가능
 * - 상단 "2026년 5월" 텍스트를 누르면 년/월 NumberPicker로 이동 가능
 * - 날짜를 누르면 해당 페이지 하단에 기록 카드를 표시함
 * - 기록 카드를 누르면 OnRecordSelectedListener 로 선택된 기록을 반환함
 */
public class FeedRecordPickerDialog {

    // ViewPager2 총 페이지 수 및 기준점 (앵커 월 기준 앞뒤로 60개월씩 = 총 120개월)
    private static final int TOTAL_PAGES = 120;
    private static final int BASE_OFFSET = 60;

    private final Context context;
    private final OnRecordSelectedListener listener;
    private Dialog dialog;

    private ViewPager2 vpCalendar;
    private TextView tvMonthYear;
    private CalendarMonthPagerAdapter pagerAdapter;
    private List<RunningRecordResponse> allServerRecords = new ArrayList<>();

    // 기준 월 — 앵커(오늘)에서 BASE_OFFSET 만큼 이전 월을 page 0 으로 잡음
    private final YearMonth anchorMonth = YearMonth.now();
    private final YearMonth baseMonth   = anchorMonth.minusMonths(BASE_OFFSET);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /*
     * 기록이 선택됐을 때 호출되는 콜백 인터페이스임
     * routeMapUri: FeedRecordDetailDialog 에서 캡처한 지도 스냅샷 URI (null 가능)
     */
    public interface OnRecordSelectedListener {
        void onRecordSelected(RunningRecordResponse record, String routeMapUri);
    }

    public FeedRecordPickerDialog(Context context, OnRecordSelectedListener listener) {
        this.context  = context;
        this.listener = listener;
    }

    /** 업로드 완료 등 외부에서 picker를 닫을 때 사용 */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_feed_record_picker);

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
            window.setDimAmount(0.7f);
        }

        // ── 뷰 바인딩 ──────────────────────────────────────────────────────
        ImageView btnBack = dialog.findViewById(R.id.btn_back_picker);
        tvMonthYear       = dialog.findViewById(R.id.tv_picker_month_year);
        vpCalendar        = dialog.findViewById(R.id.vp_picker_calendar);

        btnBack.setOnClickListener(v -> dialog.dismiss());

        // ── 달력 페이저 어댑터 ─────────────────────────────────────────────
        pagerAdapter = new CalendarMonthPagerAdapter(
                context, baseMonth, allServerRecords, listener);

        vpCalendar.setAdapter(pagerAdapter);

        // 오늘 월로 초기 위치 이동 (애니메이션 없이)
        int initPos = (int) baseMonth.until(anchorMonth, java.time.temporal.ChronoUnit.MONTHS);
        vpCalendar.setCurrentItem(initPos, false);
        updateMonthLabel(initPos);

        // ── 페이지 변경 시 헤더 월 업데이트 ───────────────────────────────
        vpCalendar.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateMonthLabel(position);
            }
        });

        // ── 월 텍스트 클릭 → 년/월 NumberPicker ──────────────────────────
        tvMonthYear.setOnClickListener(v -> showYearMonthPicker());

        // ── 서버에서 기록 불러와 달력 dot 반영 ────────────────────────────
        int userId = TokenManager.getUserId(context);
        new RunningRepository().fetchUserRecords(userId, new RunningRepository.RecordCallback() {
            @Override
            public void onSuccess(List<RunningRecordResponse> records) {
                allServerRecords = records;
                mainHandler.post(() -> {
                    if (dialog.isShowing()) {
                        pagerAdapter.setRecords(records);
                    }
                });
            }
            @Override
            public void onError(String message) { /* 조용히 실패 */ }
        });

        dialog.show();
    }

    // ── 헤더 월 텍스트 갱신 ──────────────────────────────────────────────
    private void updateMonthLabel(int position) {
        YearMonth month = baseMonth.plusMonths(position);
        tvMonthYear.setText(String.format(Locale.KOREAN, "%d년 %d월",
                month.getYear(), month.getMonthValue()));
    }

    // ── 년/월 NumberPicker 다이얼로그 (기록 탭과 동일) ─────────────────────
    private void showYearMonthPicker() {
        int curPos = vpCalendar.getCurrentItem();
        YearMonth curMonth = baseMonth.plusMonths(curPos);

        Dialog pickerDialog = new Dialog(context);
        pickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(createRoundedBg());
        root.setPadding(dp(24), dp(20), dp(24), dp(16));

        // 년/월 picker 행
        LinearLayout pickerRow = new LinearLayout(context);
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(Gravity.CENTER);
        pickerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        NumberPicker yearPicker = new NumberPicker(context);
        yearPicker.setMinValue(2024);
        yearPicker.setMaxValue(2030);
        yearPicker.setValue(curMonth.getYear());
        yearPicker.setWrapSelectorWheel(false);
        styleNumberPicker(yearPicker);
        String[] yearLabels = new String[7];
        for (int i = 0; i < 7; i++) yearLabels[i] = (2024 + i) + "년";
        yearPicker.setDisplayedValues(yearLabels);

        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(160), 1f);
        pp.setMargins(dp(8), 0, dp(8), 0);
        yearPicker.setLayoutParams(pp);

        NumberPicker monthPicker = new NumberPicker(context);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(curMonth.getMonthValue());
        monthPicker.setWrapSelectorWheel(true);
        styleNumberPicker(monthPicker);
        String[] monthLabels = new String[12];
        for (int i = 0; i < 12; i++) monthLabels[i] = (i + 1) + "월";
        monthPicker.setDisplayedValues(monthLabels);
        monthPicker.setLayoutParams(pp);

        pickerRow.addView(yearPicker);
        pickerRow.addView(monthPicker);
        root.addView(pickerRow);

        // 구분선
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divP.topMargin = dp(16);
        divider.setLayoutParams(divP);
        root.addView(divider);

        // 취소/완료 버튼 행
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        brP.topMargin = dp(12);
        btnRow.setLayoutParams(brP);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("취소");
        btnCancel.setTextColor(Color.parseColor("#888888"));
        btnCancel.setTextSize(15);
        btnCancel.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnCancel.setOnClickListener(v -> pickerDialog.dismiss());

        TextView btnConfirm = new TextView(context);
        btnConfirm.setText("완료");
        btnConfirm.setTextColor(Color.parseColor("#888888"));
        btnConfirm.setTextSize(15);
        btnConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnConfirm.setOnClickListener(v -> {
            YearMonth target = YearMonth.of(yearPicker.getValue(), monthPicker.getValue());
            long targetPos = baseMonth.until(target, java.time.temporal.ChronoUnit.MONTHS);
            targetPos = Math.max(0, Math.min(TOTAL_PAGES - 1, targetPos));
            vpCalendar.setCurrentItem((int) targetPos, true);
            pickerDialog.dismiss();
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        pickerDialog.setContentView(root);
        Window w = pickerDialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams p = w.getAttributes();
            p.width = dp(280);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }
        pickerDialog.show();
    }

    // ── NumberPicker 다크 테마 스타일 적용 (기록 탭과 동일) ──────────────────
    @SuppressLint("SoonBlockedPrivateApi")
    private void styleNumberPicker(NumberPicker picker) {
        try {
            Field paintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            paintField.setAccessible(true);
            android.graphics.Paint paint = (android.graphics.Paint) paintField.get(picker);
            if (paint != null) paint.setColor(Color.WHITE);

            Field divField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            divField.setAccessible(true);
            divField.set(picker, new ColorDrawable(Color.parseColor("#333333")));
        } catch (Exception ignored) {}

        picker.setOnValueChangedListener((p, o, n) -> {
            for (int i = 0; i < p.getChildCount(); i++) {
                View child = p.getChildAt(i);
                if (child instanceof TextView) ((TextView) child).setTextColor(Color.WHITE);
            }
        });
    }

    private GradientDrawable createRoundedBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A1A1A"));
        bg.setCornerRadius(dp(16));
        return bg;
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ViewPager2용 월별 달력 페이저 어댑터
    //  각 페이지 = item_picker_calendar_page.xml (달력 그리드 + 기록 카드 목록)
    // ════════════════════════════════════════════════════════════════════════
    private static class CalendarMonthPagerAdapter
            extends RecyclerView.Adapter<CalendarMonthPagerAdapter.PageViewHolder> {

        private final Context context;
        private final YearMonth baseMonth;
        private final OnRecordSelectedListener onRecordSelected;
        private List<RunningRecordResponse> records = new ArrayList<>();

        CalendarMonthPagerAdapter(Context ctx, YearMonth baseMonth,
                                  List<RunningRecordResponse> initialRecords,
                                  OnRecordSelectedListener onRecordSelected) {
            this.context          = ctx;
            this.baseMonth        = baseMonth;
            this.records          = initialRecords;
            this.onRecordSelected = onRecordSelected;
        }

        void setRecords(List<RunningRecordResponse> newRecords) {
            this.records = newRecords;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() { return TOTAL_PAGES; }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(context)
                    .inflate(R.layout.item_picker_calendar_page, parent, false);

            // 달력 그리드 설정
            RecyclerView rvCal = page.findViewById(R.id.rv_page_calendar);
            rvCal.setLayoutManager(new GridLayoutManager(context, 7));
            rvCal.setItemAnimator(null);
            rvCal.setHasFixedSize(true);

            // 기록 카드 RecyclerView 설정
            RecyclerView rvRecs = page.findViewById(R.id.rv_page_daily_records);
            rvRecs.setLayoutManager(new LinearLayoutManager(context));
            rvRecs.setNestedScrollingEnabled(false);

            // 기록 카드 어댑터 — 카드 클릭 시 FeedRecordDetailDialog 열어 상세 확인 후 업로드
            DailyRecordAdapter dailyAdapter = new DailyRecordAdapter(new ArrayList<>(), item -> {
                for (RunningRecordResponse res : records) {
                    if (res.getCreatedAt().equals(item.getDate())) {
                        new FeedRecordDetailDialog(context, res,
                                (record, routeMapUri) ->
                                        onRecordSelected.onRecordSelected(record, routeMapUri))
                                .show();
                        return;
                    }
                }
            });
            rvRecs.setAdapter(dailyAdapter);

            return new PageViewHolder(page, dailyAdapter);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            YearMonth month = baseMonth.plusMonths(position);

            // 페이지 재사용 시 이전 상태 초기화
            holder.tvSelectedDate.setVisibility(View.GONE);
            holder.tvNoRecord.setVisibility(View.GONE);
            holder.dailyAdapter.updateData(new ArrayList<>());

            // 달력 셀 목록 빌드 후 어댑터 교체
            List<CalendarDayItem> days = buildDays(month);
            holder.rvCalendar.setAdapter(new CalendarAdapter(days, day -> {
                if (day == null) return;
                LocalDate date = day.getDate();

                // 선택된 날짜 레이블
                String label = date.getYear() + "년 " + date.getMonthValue() + "월 "
                        + date.getDayOfMonth() + "일 "
                        + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
                holder.tvSelectedDate.setText(label);
                holder.tvSelectedDate.setVisibility(View.VISIBLE);

                // 해당 날짜 기록 필터링
                List<RunningRecordItem> filtered = filterByDate(date);
                if (filtered.isEmpty()) {
                    holder.tvNoRecord.setVisibility(View.VISIBLE);
                    holder.dailyAdapter.updateData(new ArrayList<>());
                } else {
                    holder.tvNoRecord.setVisibility(View.GONE);
                    holder.dailyAdapter.updateData(filtered);
                }
            }));
        }

        // ── 해당 월의 달력 셀 목록 생성 (앞 빈칸 null 포함 + 거리 dot) ──────
        private List<CalendarDayItem> buildDays(YearMonth month) {
            List<CalendarDayItem> list = new ArrayList<>();
            LocalDate first = month.atDay(1);
            int startDow = first.getDayOfWeek().getValue() % 7;
            for (int i = 0; i < startDow; i++) list.add(null);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            for (int d = 1; d <= month.lengthOfMonth(); d++) {
                LocalDate date = month.atDay(d);
                CalendarDayItem item = new CalendarDayItem(date, "", true);
                float total = 0f;
                for (RunningRecordResponse res : records) {
                    try {
                        LocalDate rd = LocalDateTime.parse(res.getCreatedAt(), fmt)
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                .toLocalDate();
                        if (rd.equals(date)) total += res.getDistance();
                    } catch (Exception ignored) {}
                }
                if (total > 0)
                    item.setDistance(String.format(Locale.getDefault(), "%.2fkm", total));
                list.add(item);
            }
            return list;
        }

        // ── 특정 날짜의 기록 카드 목록 반환 ─────────────────────────────────
        private List<RunningRecordItem> filterByDate(LocalDate date) {
            List<RunningRecordItem> result = new ArrayList<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            for (RunningRecordResponse res : records) {
                try {
                    LocalDate resDate = LocalDateTime.parse(res.getCreatedAt(), fmt)
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                            .toLocalDate();
                    if (resDate.equals(date)) {
                        RunningRecordItem item = convertToItem(res);
                        item.setAiCoaching(res.getPlanId() != null);
                        result.add(item);
                    }
                } catch (Exception ignored) {}
            }
            return result;
        }

        private RunningRecordItem convertToItem(RunningRecordResponse res) {
            int totalSec = (int) res.getTime();
            String timeStr = String.format(Locale.getDefault(),
                    "%02d:%02d", totalSec / 60, totalSec % 60);
            int paceSec = res.getPace() < 60
                    ? (int) (res.getPace() * 60) : (int) res.getPace();
            String paceStr = String.format(Locale.getDefault(),
                    "%d:%02d/km", paceSec / 60, paceSec % 60);
            return new RunningRecordItem(
                    res.getCreatedAt(),
                    String.format(Locale.getDefault(), "%.2fkm", res.getDistance()),
                    timeStr, paceStr,
                    (int) res.getCalories() + "kcal"
            );
        }

        // ── PageViewHolder : item_picker_calendar_page.xml 의 뷰들을 보관 ──
        static class PageViewHolder extends RecyclerView.ViewHolder {
            final RecyclerView rvCalendar;
            final TextView tvSelectedDate;
            final TextView tvNoRecord;
            final RecyclerView rvDailyRecords;
            final DailyRecordAdapter dailyAdapter;

            PageViewHolder(View itemView, DailyRecordAdapter adapter) {
                super(itemView);
                rvCalendar     = itemView.findViewById(R.id.rv_page_calendar);
                tvSelectedDate = itemView.findViewById(R.id.tv_page_selected_date);
                tvNoRecord     = itemView.findViewById(R.id.tv_page_no_record);
                rvDailyRecords = itemView.findViewById(R.id.rv_page_daily_records);
                dailyAdapter   = adapter;
            }
        }
    }
}
