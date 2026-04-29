package com.neostride.app.feature.record;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class RecordPagerAdapter extends FragmentStateAdapter {
    private final YearMonth baseMonth = YearMonth.of(2024, 1); // 기준 달
    private final int PAGE_COUNT = 240; // 20년치

    public RecordPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 해당 인덱스에 맞는 월 프래그먼트 생성
        YearMonth targetMonth = baseMonth.plusMonths(position);
        return MonthPageFragment.newInstance(targetMonth);
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