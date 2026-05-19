package com.neostride.app.feature.main.running.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

//  GPS 좌표 단일 포인트 요청/응답 공용 DTO
//  <p>
//  - 위도·경도·타임스탬프 문자열을 담으며 저장 요청과 경로 응답에 동시 사용된다.
//  - heartRate·cadence 는 스마트워치 연동 시 채워지는 선택 필드 (없으면 null).
public class GpsTraceRequest implements Serializable {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("time")
    private String time;

    // 워치 연동 시 사용 — 현재는 null로 전송, 서버 응답에 포함될 때도 자동 매핑됨
    @SerializedName("heart_rate")
    private Integer heartRate;   // bpm (nullable)

    @SerializedName("cadence")
    private Integer cadence;     // spm (nullable)

    public GpsTraceRequest(double latitude, double longitude, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTime() { return time; }
    public Integer getHeartRate() { return heartRate; }
    public Integer getCadence() { return cadence; }
}