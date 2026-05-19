package com.neostride.app.feature.main.running.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;


//  러닝 기록 저장 요청 DTO
//  <p>
//  - 사용자 ID·플랜 ID·거리·소요 시간·페이스·칼로리·경로·GPS 좌표 목록·배지 등급을 담는다.
//  - planId가 null이면 일반 러닝, 값이 있으면 AI 코칭 러닝으로 처리된다.

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
    private int pace;

    @SerializedName("calories")
    private float calories;

    @SerializedName("route_detail")
    private String routeDetail;

    @SerializedName("gps_traces")
    private List<GpsTraceRequest> gpsTraces; // 명칭 통일 완료

    @SerializedName("badge")
    private String badge;

    public RunningRecordRequest(int userId, Integer planId, float totalDistance,
                                int duration, int pace, float calories,
                                String routeDetail, List<GpsTraceRequest> gpsTraces,
                                String badge) {
        this.userId = userId;
        this.planId = planId;
        this.totalDistance = totalDistance;
        this.duration = duration;
        this.pace = pace;
        this.calories = calories;
        this.routeDetail = routeDetail;
        this.gpsTraces = gpsTraces;
        this.badge = badge;
    }
}
