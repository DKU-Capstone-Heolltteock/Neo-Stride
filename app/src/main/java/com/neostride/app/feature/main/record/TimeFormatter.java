package com.neostride.app.feature.main.record;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeFormatter {

    private static final DateTimeFormatter DATE_DOT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private TimeFormatter() { /* no instances */ }

    public static String format(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(isoTime);
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();

            if (dt.toLocalDate().isEqual(today)) {
                long diffSec = Duration.between(dt, now).getSeconds();
                if (diffSec < 0) return "방금 전";
                if (diffSec < 60) return "방금 전";
                long diffMin = diffSec / 60;
                if (diffMin < 60) return diffMin + "분 전";
                long diffHour = diffMin / 60;
                return diffHour + "시간 전";
            }
            return dt.format(DATE_DOT_FORMAT);
        } catch (Exception e) {
            String datePart = isoTime.length() >= 10 ? isoTime.substring(0, 10) : isoTime;
            return datePart.replace("-", ".");
        }
    }
}
