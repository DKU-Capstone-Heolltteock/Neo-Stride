package com.neostride.app.feature.record;

import java.io.Serializable;

public class RunningRecordItem implements Serializable {
    private String date;
    private String distance;
    private String time;
    private String pace;
    private String calories;
    private boolean aiCoaching; // AI 코칭 목표로 달린 기록인지

    public RunningRecordItem(String date, String distance, String time, String pace, String calories) {
        this.date = date;
        this.distance = distance;
        this.time = time;
        this.pace = pace;
        this.calories = calories;
        this.aiCoaching = false;
    }

    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getPace() { return pace; }
    public String getCalories() { return calories; }
    public boolean isAiCoaching() { return aiCoaching; }
    public void setAiCoaching(boolean aiCoaching) { this.aiCoaching = aiCoaching; }
}