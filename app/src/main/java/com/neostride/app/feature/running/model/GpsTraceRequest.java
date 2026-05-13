package com.neostride.app.feature.running.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

//  GPS 좌표 단일 포인트 요청/응답 공용 DTO
//  <p>
//  - 위도·경도·타임스탬프 문자열을 담으며 저장 요청과 경로 응답에 동시 사용된다.
public class GpsTraceRequest implements Serializable {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("time")
    private String time;

    public GpsTraceRequest(double latitude, double longitude, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTime() { return time; }
}