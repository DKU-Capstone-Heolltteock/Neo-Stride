package com.neostride.app.feature.main.record;

import java.io.Serializable;


//  하루 러닝 기록 데이터 모델 (RecordFragment·DailyRecordAdapter에서 사용)
//  <p>
//  - 날짜·거리·시간·페이스·칼로리와 AI 코칭 여부를 담는다.

public class RunningRecordItem implements Serializable {
    private long id;
    private String date;
    private String distance;
    private String time;
    private String pace;
    private String calories;
    // AI 코칭 목표 기반 기록 여부
    private boolean aiCoaching;

    public RunningRecordItem(String date, String distance, String time, String pace, String calories) {
        this.id = 0;
        this.date = date;
        this.distance = distance;
        this.time = time;
        this.pace = pace;
        this.calories = calories;
        this.aiCoaching = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getPace() { return pace; }
    public String getCalories() { return calories; }
    public boolean isAiCoaching() { return aiCoaching; }
    public void setAiCoaching(boolean aiCoaching) { this.aiCoaching = aiCoaching; }
}