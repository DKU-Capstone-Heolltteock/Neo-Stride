package com.neostride.app.feature.badge.model;

import android.graphics.Color;


//  뱃지 등급 열거형
//  <p>
//  - 각 등급별 한글 이름·색상 코드 보유
//  - 거리(km)·페이스(초/km) 조합으로 등급을 판정하는 {@link #getTierNameByRecord} 제공
//  - 인접 기준점 사이를 선형 보간({@link #interpolate})해 등급 경계를 산출

public enum BadgeTier {
    NONE("UnRanked", "#AAAAAA"),
    BRONZE("브론즈", "#A46628"),
    SILVER("실버", "#A3B3B7"),
    GOLD("골드", "#FFD700"),
    PLATINUM("플래티넘", "#27A0A8"),
    DIAMOND("다이아", "#00BFFF"),
    MASTER("마스터", "#B000FF"),
    CHALLENGER("챌린저", "#FF007F");

    private final String name;
    private final String colorCode;

    BadgeTier(String name, String colorCode) {
        this.name = name;
        this.colorCode = colorCode;
    }

    public String getName() { return name; }
    public int getColor() { return Color.parseColor(colorCode); }

    /** 언랭(UnRanked) 여부를 반환한다. true이면 배지 아이콘을 숨겨야 한다. */
    public boolean isNone() { return this == NONE; }

    public static BadgeTier fromString(String text) {
        if (text == null) return NONE;
        for (BadgeTier b : BadgeTier.values()) {
            if (b.name().equalsIgnoreCase(text)) return b;
        }
        return NONE;
    }

    /**
     * 거리(km)와 페이스(초/km)로 등급 문자열을 반환한다.
     *
     * @param inputKm       달린 거리 (km)
     * @param inputPaceSec  페이스 (초/km)
     * @return 등급 소문자 문자열 (예: "gold", "none")
     */
    public static String getTierNameByRecord(double inputKm, int inputPaceSec) {
        double[] dists = {1.0, 3.0, 5.0, 10.0, 20.0, 40.0};

        // 액티비티에 있던 원본 데이터들
        int[] bronze = {340, 360, 380, 410, 450, 510};
        int[] silver = {315, 335, 355, 385, 425, 485};
        int[] gold = {290, 310, 330, 360, 400, 460};
        int[] platinum = {270, 290, 310, 340, 380, 440};
        int[] diamond = {250, 270, 290, 320, 360, 420};
        int[] master = {230, 250, 270, 300, 340, 400};
        int[] challenger = {210, 230, 250, 280, 320, 380};

        int i = 0;
        if (inputKm <= dists[0]) i = 0;
        else if (inputKm >= dists[dists.length - 1]) i = dists.length - 2;
        else {
            while (i < dists.length - 2 && inputKm > dists[i + 1]) i++;
        }

        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], challenger[i], challenger[i+1])) return "challenger";
        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], master[i], master[i+1])) return "master";
        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], diamond[i], diamond[i+1])) return "diamond";
        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], platinum[i], platinum[i+1])) return "platinum";
        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], gold[i], gold[i+1])) return "gold";
        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], silver[i], silver[i+1])) return "silver";
        if (inputPaceSec <= interpolate(inputKm, dists[i], dists[i+1], bronze[i], bronze[i+1])) return "bronze";

        return "none";
    }

    /** 두 기준점 (x1,y1)~(x2,y2) 사이에서 x에 대한 선형 보간값을 반환한다. */
    private static double interpolate(double x, double x1, double x2, double y1, double y2) {
        if (x <= x1) return y1;
        if (x >= x2) return y2;
        return y1 + (x - x1) * (y2 - y1) / (x2 - x1);
    }
}