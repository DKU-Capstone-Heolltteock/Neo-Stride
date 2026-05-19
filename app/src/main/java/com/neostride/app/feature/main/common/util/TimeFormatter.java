package com.neostride.app.feature.main.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/*
 * ISO 8601 시간 문자열을 화면 표시용으로 변환하는 유틸 클래스임
 * 오늘 내 시간은 상대 표시(방금 전, N분 전, N시간 전),
 * 오늘 이전 날짜는 절대 날짜(yyyy.MM.dd) 로 반환함
 */
public final class TimeFormatter {

    private TimeFormatter() { /* no instances */ }

    public static String format(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime created = LocalDateTime.parse(
                    isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ZonedDateTime createdKst = created.atZone(ZoneId.of("Asia/Seoul"));
            ZonedDateTime nowKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

            long minutes = ChronoUnit.MINUTES.between(createdKst, nowKst);

            if (minutes < 1) {
                return "방금 전";
            } else if (minutes < 60) {
                return minutes + "분 전";
            } else if (minutes < 60 * 24) {
                return (minutes / 60) + "시간 전";
            } else {
                return created.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            }
        } catch (Exception e) {
            return isoTime;
        }
    }
}
