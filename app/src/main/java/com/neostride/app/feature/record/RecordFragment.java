package com.neostride.app.feature.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.neostride.app.R;
import java.time.YearMonth;

public class RecordFragment extends Fragment {
    private ViewPager2 viewPager;
    private RecordPagerAdapter pagerAdapter;
    private TextView tvMonthYear;
    private final YearMonth baseMonth = YearMonth.of(2024, 1);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        tvMonthYear = view.findViewById(R.id.tv_month_year);
        viewPager = view.findViewById(R.id.view_pager_records);
        ImageView btnPrev = view.findViewById(R.id.btn_prev_month);
        ImageView btnNext = view.findViewById(R.id.btn_next_month);

        pagerAdapter = new RecordPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // 현재 월로 시작 위치 설정
        int currentPos = pagerAdapter.getPositionForMonth(YearMonth.now());
        viewPager.setCurrentItem(currentPos, false);

        // 페이지 변경 시 헤더 텍스트 업데이트
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                YearMonth selectedMonth = baseMonth.plusMonths(position);
                tvMonthYear.setText(String.format("%d년 %d월", selectedMonth.getYear(), selectedMonth.getMonthValue()));
            }
        });

        // 버튼 클릭 시 페이지 이동 (애니메이션 true)
        btnPrev.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true));
        btnNext.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true));

        return view;
    }
}