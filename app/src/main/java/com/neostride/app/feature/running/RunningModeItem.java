package com.neostride.app.feature.running;

public class RunningModeItem {
    public String title;
    public String subtitle;
    public int bgColor;
    public boolean isCoaching;

    public RunningModeItem(String title, String subtitle, int bgColor, boolean isCoaching) {
        this.title = title;
        this.subtitle = subtitle;
        this.bgColor = bgColor;
        this.isCoaching = isCoaching;
    }
}