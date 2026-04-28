package com.neostride.app.feature.running.model;

import com.google.gson.annotations.SerializedName;


//RunningRecordResponse.java
//서버에 기록 저장을 요청한 후 돌아오는 응답 데이터를 담는 DTO

public class RunningRecordResponse {
    @SerializedName("status")
    private String status;      // 성공 여부 (success / error)

    @SerializedName("message")
    private String message;     // 서버에서 보내는 메시지

    @SerializedName("run_record_id")
    private int runRecordId;    // DB에 저장된 기록의 고유 번호 (결과 상세 화면 이동 시 사용)

    // 서버의 응답을 확인하기 위한 Getter 메서드들
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getRunRecordId() { return runRecordId; }
}