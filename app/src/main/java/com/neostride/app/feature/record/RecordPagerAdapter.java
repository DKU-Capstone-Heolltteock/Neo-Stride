package com.neostride.app.feature.record;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/*
 * 기록 캘린더 ViewPager 어댑터 클래스임
 */
public class RecordPagerAdapter extends FragmentStateAdapter {

    private final YearMonth baseMonth = YearMonth.of(2024, 1);

    // 20년치 페이지
    private final int PAGE_COUNT = 240;

    // 팁 GPS 선택 모드 여부
    private final boolean isTipMode;

    public RecordPagerAdapter(
            @NonNull Fragment fragment,
            boolean isTipMode
    ) {
        super(fragment);

        this.isTipMode = isTipMode;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {

        // 현재 페이지에 해당하는 월 계산
        YearMonth targetMonth = baseMonth.plusMonths(position);

        // 팁 모드 여부를 MonthPageFragment에 전달
        return MonthPageFragment.newInstance(
                targetMonth,
                isTipMode
        );
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    /*
     * 특정 월이 몇 번째 페이지인지 계산하는 함수임
     */
    public int getPositionForMonth(YearMonth month) {

        return (int) ChronoUnit.MONTHS.between(
                baseMonth,
                month
        );
    }
}