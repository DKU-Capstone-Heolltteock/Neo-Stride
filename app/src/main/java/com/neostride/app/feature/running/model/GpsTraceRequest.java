package com.neostride.app.feature.running.model;

// ============================================================
// GpsTraceRequest.java
// GPS 좌표 하나하나의 위도, 경도, 시간 정보를 담는 DTO
// ============================================================

import com.google.gson.annotations.SerializedName;

public class GpsTraceRequest {
    @SerializedName("latitude")
    private double latitude;  // 위도 (DB: latitude)

    @SerializedName("longitude")
    private double longitude; // 경도 (DB: longitude)

    @SerializedName("time")
    private String time;      // 측정 시간 (DB: time, 포맷: yyyy-MM-dd HH:mm:ss)

    // GPS 객체 생성을 위한 생성자
    public GpsTraceRequest(double latitude, double longitude, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    // 서버 전송 및 데이터 확인을 위한 Getter 메서드
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTime() { return time; }
}