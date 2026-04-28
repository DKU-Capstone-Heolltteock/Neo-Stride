package com.neostride.app.feature.record;

import java.io.Serializable;

public class RunningRecord implements Serializable {
    private String date;      // "3월 20일 목요일"
    private String distance;  // "7km"
    private String time;      // "37:49"
    private String pace;      // "6:24/km"
    private String calories;  // "453kcal"

    public RunningRecord(String date, String distance, String time, String pace, String calories) {
        this.date = date;
        this.distance = distance;
        this.time = time;
        this.pace = pace;
        this.calories = calories;
    }

    // Getter들
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getPace() { return pace; }
    public String getCalories() { return calories; }
}