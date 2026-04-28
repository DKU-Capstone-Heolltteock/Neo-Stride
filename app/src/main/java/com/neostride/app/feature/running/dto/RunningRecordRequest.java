package com.neostride.app.feature.running.dto;

// ============================================================
// RunningRecordRequest.java
// 전체 러닝 세션 데이터를 담아 서버로 전송하는 메인 DTO
// ============================================================

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RunningRecordRequest {
    @SerializedName("user_id")
    private int userId;         // 사용자 고유 식별자

    @SerializedName("plan_id")
    private Integer planId;     // 연동된 코칭 플랜 ID (없을 시 null)

    @SerializedName("total_distance")
    private float totalDistance; // 총 주행 거리 (km)

    @SerializedName("duration")
    private float duration;      // 총 주행 시간 (초)

    @SerializedName("pace")
    private float pace;          // 평균 페이스 (분/km)

    @SerializedName("calories")
    private float calories;      // 소모 칼로리 (kcal)

    @SerializedName("gps_traces")
    private List<GpsTraceRequest> gpsTraces; // 상세 경로 좌표 리스트 (1:N)

    // 최종 데이터 조립용 생성자
    public RunningRecordRequest(int userId, Integer planId, float totalDistance,
                                float duration, float pace, float calories,
                                List<GpsTraceRequest> gpsTraces) {
        this.userId = userId;
        this.planId = planId;
        this.totalDistance = totalDistance;
        this.duration = duration;
        this.pace = pace;
        this.calories = calories;
        this.gpsTraces = gpsTraces;
    }
}