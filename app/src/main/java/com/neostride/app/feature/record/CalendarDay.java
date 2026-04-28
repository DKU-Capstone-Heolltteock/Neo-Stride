package com.neostride.app.feature.record;

import java.time.LocalDate;

public class CalendarDay {
    private LocalDate date;
    private String distance;
    private boolean isSelected;
    private boolean isCurrentMonth;
    private String coachingStatus; // null / "pending" / "completed" / "missed"

    public CalendarDay(LocalDate date, String distance, boolean isCurrentMonth) {
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