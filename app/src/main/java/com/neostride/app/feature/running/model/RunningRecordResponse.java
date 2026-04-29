package com.neostride.app.feature.running.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * 서버 응답 DTO — 저장 응답 + 조회 응답 통합
 * ERD: RUNNING_RECORDS 테이블 매핑
 */
public class RunningRecordResponse implements Serializable {

    // ── 저장 응답 ──
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("run_record_id")
    private int runRecordId;

    // ── 조회 응답 (RUNNING_RECORDS 테이블) ──
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

    // ── Getter (MonthPageFragment + RecordDetailFragment에서 사용하는 이름 그대로) ──
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getRunRecordId() { return runRecordId; }
    public String getCreatedAt() { return createdAt; }
    public float getDistance() { return distance; }
    public float getTime() { return time; }
    public float getPace() { return pace; }
    public float getCalories() { return calories; }
    public List<GpsTraceRequest> getGpsPath() { return gpsPath; }
    public List<Float> getSegmentPaces() { return segmentPaces; }
}