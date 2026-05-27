package com.neostride.app.feature.main.running;


//  러닝 모드 선택 카드 데이터 모델
//  <p>
//  - 카드 제목·부제목·배경색·AI 코칭 여부를 담는다.
public class RunningModeItem {
    public String title;
    public String subtitle;
    public int bgColor;
    public boolean isCoaching;
    public boolean isEnabled; // GPS 준비 전에는 false → 회색 비활성 상태

    public RunningModeItem(String title, String subtitle, int bgColor, boolean isCoaching) {
        this.title = title;
        this.subtitle = subtitle;
        this.bgColor = bgColor;
        this.isCoaching = isCoaching;
        this.isEnabled = true; // 기본값: 활성
    }

    public RunningModeItem(String title, String subtitle, int bgColor, boolean isCoaching, boolean isEnabled) {
        this.title = title;
        this.subtitle = subtitle;
        this.bgColor = bgColor;
        this.isCoaching = isCoaching;
        this.isEnabled = isEnabled;
    }
}