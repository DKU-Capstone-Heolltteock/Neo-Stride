package com.neostride.app.feature.record;

import java.time.LocalDate;


//  기록 탭 캘린더 날짜 셀 데이터 모델
//  <p>
//  - 날짜, 러닝 거리, 현재 월 여부, 코칭 상태(null/"pending"/"completed"/"missed")를 담는다.

public class CalendarDayItem {
    private LocalDate date;
    private String distance;
    private boolean isSelected;
    private boolean isCurrentMonth;
    private String coachingStatus; // null | "pending" | "completed" | "missed"

    public CalendarDayItem(LocalDate date, String distance, boolean isCurrentMonth) {
        this.date = date;
        this.distance = distance;
        this.isCurrentMonth = isCurrentMonth;
        this.isSelected = false;
        this.coachingStatus = null;
    }

    public LocalDate getDate() { return date; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public boolean isCurrentMonth() { return isCurrentMonth; }
    public String getCoachingStatus() { return coachingStatus; }
    public void setCoachingStatus(String status) { this.coachingStatus = status; }
    public boolean hasDistance() { return distance != null && !distance.isEmpty(); }
}