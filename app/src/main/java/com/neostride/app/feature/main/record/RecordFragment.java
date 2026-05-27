package com.neostride.app.feature.main.record;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.neostride.app.R;

import java.lang.reflect.Field;
import java.time.YearMonth;
import android.annotation.SuppressLint;


//  기록 탭 Fragment
//  <p>
//  - ViewPager2로 월별 기록 페이지를 표시한다.
//  - 상단 년/월 텍스트 클릭 시 NumberPicker 다이얼로그로 원하는 달로 이동할 수 있다.

public class RecordFragment extends Fragment {

    // ── ViewPager2 및 어댑터 ──
    private ViewPager2 viewPager;
    private RecordPagerAdapter pagerAdapter;
    private TextView tvMonthYear;
    // 페이지 인덱스 계산 기준 월 (2024년 1월)
    private final YearMonth baseMonth = YearMonth.of(2024, 1);

    private boolean isTipMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_record, container, false);
        if (getArguments() != null) {
            isTipMode = getArguments().getBoolean("tip_mode", false);
        }

        tvMonthYear = view.findViewById(R.id.tv_month_year);
        viewPager = view.findViewById(R.id.view_pager_records);

        pagerAdapter = new RecordPagerAdapter(this, isTipMode);
        viewPager.setAdapter(pagerAdapter);

        int currentPos = pagerAdapter.getPositionForMonth(YearMonth.now());
        viewPager.setCurrentItem(currentPos, false);

        // 초기 헤더: 현재 월
        YearMonth now = YearMonth.now();
        tvMonthYear.setText(String.format("%d년 %d월", now.getYear(), now.getMonthValue()));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                YearMonth selectedMonth = baseMonth.plusMonths(position);
                tvMonthYear.setText(String.format("%d년 %d월", selectedMonth.getYear(), selectedMonth.getMonthValue()));
            }
        });

        tvMonthYear.setOnClickListener(v -> showYearMonthPicker());

        return view;
    }

    // MainActivity가 add()+hide()/show() 패턴으로 fragment를 보존하므로,
    // 새 측정이 끝나고 기록 탭으로 돌아와도 onResume/onCreateView가 호출되지 않는다.
    // 다시 보일 때 attached된 MonthPageFragment들을 refresh해 최신 기록을 반영한다.
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            for (Fragment f : getChildFragmentManager().getFragments()) {
                if (f instanceof MonthPageFragment && f.isAdded()) {
                    ((MonthPageFragment) f).refresh();
                }
            }
        }
    }

    // ─── 년/월 선택 NumberPicker 다이얼로그 표시 ───
    private void showYearMonthPicker() {
        int currentPos = viewPager.getCurrentItem();
        YearMonth currentMonth = baseMonth.plusMonths(currentPos);

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 다크 테마 레이아웃 직접 생성
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1A1A1A"));
        root.setPadding(dp(24), dp(20), dp(24), dp(16));

        // 년/월 스피너 영역
        LinearLayout pickerRow = new LinearLayout(requireContext());
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(Gravity.CENTER);
        pickerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 년 NumberPicker
        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(2024);
        yearPicker.setMaxValue(2030);
        yearPicker.setValue(currentMonth.getYear());
        yearPicker.setWrapSelectorWheel(false);
        styleNumberPicker(yearPicker);

        String[] yearLabels = new String[7];
        for (int i = 0; i < 7; i++) yearLabels[i] = (2024 + i) + "년";
        yearPicker.setDisplayedValues(yearLabels);

        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(
                0, dp(160), 1f);
        pickerParams.setMargins(dp(8), 0, dp(8), 0);
        yearPicker.setLayoutParams(pickerParams);

        // 월 NumberPicker
        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentMonth.getMonthValue());
        monthPicker.setWrapSelectorWheel(true);
        styleNumberPicker(monthPicker);

        String[] monthLabels = new String[12];
        for (int i = 0; i < 12; i++) monthLabels[i] = (i + 1) + "월";
        monthPicker.setDisplayedValues(monthLabels);
        monthPicker.setLayoutParams(pickerParams);

        pickerRow.addView(yearPicker);
        pickerRow.addView(monthPicker);
        root.addView(pickerRow);

        // 구분선
        View divider = new View(requireContext());
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.topMargin = dp(16);
        divider.setLayoutParams(divParams);
        root.addView(divider);

        // 취소/완료 버튼
        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = dp(12);
        btnRow.setLayoutParams(btnRowParams);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("취소");
        btnCancel.setTextColor(Color.parseColor("#888888"));
        btnCancel.setTextSize(15);
        btnCancel.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnCancel.setOnClickListener(v2 -> dialog.dismiss());

        TextView btnConfirm = new TextView(requireContext());
        btnConfirm.setText("완료");
        btnConfirm.setTextColor(Color.parseColor("#888888"));
        btnConfirm.setTextSize(15);
        btnConfirm.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnConfirm.setOnClickListener(v2 -> {
            YearMonth target = YearMonth.of(yearPicker.getValue(), monthPicker.getValue());
            int targetPos = pagerAdapter.getPositionForMonth(target);
            viewPager.setCurrentItem(targetPos, true);
            dialog.dismiss();
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnConfirm);
        root.addView(btnRow);

        dialog.setContentView(root);

        // 다이얼로그 크기/위치 설정
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dp(280);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            // 둥근 모서리
            root.setBackground(createRoundedBg());
        }

        dialog.show();
    }

    // ─── NumberPicker 텍스트·구분선 색상을 다크 테마(흰색/회색)로 적용 (리플렉션 사용) ───
    @SuppressLint("SoonBlockedPrivateApi")
    private void styleNumberPicker(NumberPicker picker) {
        try {
            Field selectorWheelPaintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            android.graphics.Paint paint = (android.graphics.Paint) selectorWheelPaintField.get(picker);
            if (paint != null) paint.setColor(Color.WHITE);

            for (int i = 0; i < picker.getChildCount(); i++) {
                View child = picker.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(Color.WHITE);
                }
            }

            // 구분선 색상
            Field selDivField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            selDivField.setAccessible(true);
            selDivField.set(picker, new ColorDrawable(Color.parseColor("#333333")));
        } catch (Exception e) {
            // 일부 기기에서 리플렉션 실패해도 기능은 동작
        }

        picker.setOnValueChangedListener((p, oldVal, newVal) -> {
            // 값 변경 시 텍스트 색상 재적용
            for (int i = 0; i < p.getChildCount(); i++) {
                View child = p.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(Color.WHITE);
                }
            }
        });
    }

    // ─── 다이얼로그 다크 배경용 둥근 GradientDrawable 생성 ───
    private android.graphics.drawable.GradientDrawable createRoundedBg() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#1A1A1A"));
        bg.setCornerRadius(dp(16));
        return bg;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    public static RecordFragment newInstance(boolean isTipMode) {
        RecordFragment fragment = new RecordFragment();

        Bundle args = new Bundle();
        args.putBoolean("tip_mode", isTipMode);

        fragment.setArguments(args);

        return fragment;
    }
}