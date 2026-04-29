package com.neostride.app.feature.running.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * GPS 좌표 하나하나의 위도, 경도, 시간 정보를 담는 요청/응답 공용 DTO
 */
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