package com.neostride.app.feature.coaching;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Calendar;

/**
 * 주간 캘린더용 ViewPager2 어댑터
 * 기준: 2024년 1월 첫째주부터 시작, 총 500주 분량
 * 현재 주가 중간(250)에 위치
 */
public class WeekPagerAdapter extends FragmentStateAdapter {

    private static final int TOTAL_WEEKS = 500;
    private static final int CENTER_POSITION = 250;
    private final Calendar baseWeekStart;

    public WeekPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
        // 기준: 현재 주
        baseWeekStart = Calendar.getInstance();
        setToStartOfWeek(baseWeekStart);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int weekOffset = position - CENTER_POSITION;
        Calendar weekStart = (Calendar) baseWeekStart.clone();
        weekStart.add(Calendar.WEEK_OF_YEAR, weekOffset);
        return WeekPageFragment.newInstance(weekStart.getTimeInMillis());
    }

    @Override
    public int getItemCount() {
        return TOTAL_WEEKS;
    }

    public int getCurrentWeekPosition() {
        return CENTER_POSITION;
    }

    /**
     * 특정 날짜가 속한 주의 position 반환
     */
    public int getPositionForDate(int year, int month, int day) {
        Calendar target = Calendar.getInstance();
        target.set(Calendar.YEAR, year);
        target.set(Calendar.MONTH, month - 1);
        target.set(Calendar.DAY_OF_MONTH, day);
        setToStartOfWeek(target);

        long diffMillis = target.getTimeInMillis() - baseWeekStart.getTimeInMillis();
        int diffWeeks = (int) (diffMillis / (7L * 24 * 60 * 60 * 1000));
        return CENTER_POSITION + diffWeeks;
    }

    private void setToStartOfWeek(Calendar cal) {
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = dayOfWeek - Calendar.SUNDAY;
        cal.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * position으로부터 해당 주의 시작 Calendar 반환
     */
    public Calendar getWeekStartForPosition(int position) {
        int weekOffset = position - CENTER_POSITION;
        Calendar weekStart = (Calendar) baseWeekStart.clone();
        weekStart.add(Calendar.WEEK_OF_YEAR, weekOffset);
        return weekStart;
    }
}