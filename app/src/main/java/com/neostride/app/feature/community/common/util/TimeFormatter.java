package com.neostride.app.feature.community.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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