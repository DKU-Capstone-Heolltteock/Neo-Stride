package com.neostride.app.feature.running.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RunningRecordRequest {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("plan_id")
    private Integer planId;

    @SerializedName("total_distance")
    private float totalDistance;

    @SerializedName("duration")
    private int duration; // 초 단위

    @SerializedName("pace")
    private float pace;

    @SerializedName("calories")
    private float calories;

    @SerializedName("route_detail")
    private String routeDetail;

    @SerializedName("gps_traces")
    private List<GpsTraceRequest> gpsTraces; // 명칭 통일 완료

    public RunningRecordRequest(int userId, Integer planId, float totalDistance,
                                int duration, float pace, float calories,
                                String routeDetail, List<GpsTraceRequest> gpsTraces) {
        this.userId = userId;
        this.planId = planId;
        this.totalDistance = totalDistance;
        this.duration = duration;
        this.pace = pace;
        this.calories = calories;
        this.routeDetail = routeDetail;
        this.gpsTraces = gpsTraces;
    }
}