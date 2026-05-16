package com.neostride.app.feature.record;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;


//  월별 기록 ViewPager2 어댑터
//  <p>
//  - 기준 월(2024년 1월)부터 총 240페이지(20년)를 생성한다.
//  - isTipMode를 MonthPageFragment에 전달해 팁 GPS 선택 모드를 지원한다.
//  - {@link #getPositionForMonth}로 특정 월의 페이지 인덱스를 계산한다.

public class RecordPagerAdapter extends FragmentStateAdapter {

    private final YearMonth baseMonth = YearMonth.of(2024, 1);
    private final int PAGE_COUNT = 240;
    private final boolean isTipMode;

    public RecordPagerAdapter(@NonNull Fragment fragment, boolean isTipMode) {
        super(fragment);
        this.isTipMode = isTipMode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        YearMonth targetMonth = baseMonth.plusMonths(position);
        return MonthPageFragment.newInstance(targetMonth, isTipMode);
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    // 특정 월이 몇 번째 페이지인지 계산
    public int getPositionForMonth(YearMonth month) {
        return (int) ChronoUnit.MONTHS.between(baseMonth, month);
    }
}