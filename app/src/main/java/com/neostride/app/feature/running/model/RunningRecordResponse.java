package com.neostride.app.feature.running.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;


//  러닝 기록 서버 응답 DTO (저장 응답·조회 응답 통합)
//  <p>
//  - ERD의 RUNNING_RECORDS 테이블과 매핑된다.
//  - planId가 null이면 일반 러닝, 값이 있으면 AI 코칭 러닝을 의미한다.
//  - {@link #isCoachingRun()} 편의 메서드로 코칭 여부를 확인할 수 있다.

public class RunningRecordResponse implements Serializable {

    // ── 저장 응답 ──
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("run_record_id")
    private int runRecordId;

    // ── 조회 응답 (RUNNING_RECORDS 테이블) ──

    // 추가된 필드: Integer를 사용하여 null(일반러닝) 허용
    @SerializedName("plan_id")
    private Integer planId;

    @SerializedName("created_at")
    private String createdAt;              // "2026-04-28T14:30:00"

    @SerializedName("total_distance")
    private float distance;                // km

    @SerializedName("duration")
    private float time;                    // 초

    @SerializedName("pace")
    private float pace;                    // 분/km

    @SerializedName("calories")
    private float calories;                // kcal

    @SerializedName("gps_traces")
    private List<GpsTraceRequest> gpsPath; // GPS 경로

    @SerializedName("segment_paces")
    private List<Float> segmentPaces;      // 구간별 페이스

    // ── Getter ──
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getRunRecordId() { return runRecordId; }

    // planId Getter
    public Integer getPlanId() { return planId; }

    public String getCreatedAt() { return createdAt; }
    public float getDistance() { return distance; }
    public float getTime() { return time; }
    public float getPace() { return pace; }
    public float getCalories() { return calories; }
    public List<GpsTraceRequest> getGpsPath() { return gpsPath; }
    public List<Float> getSegmentPaces() { return segmentPaces; }

//      편의 메서드: 코칭 기록인지 확인
//      planId가 null이 아니면 코칭 러닝으로 판단합니다.

    public boolean isCoachingRun() {
        return planId != null;
    }
}