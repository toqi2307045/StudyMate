package com.example.studymate.util;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class TimeUtil {
    private TimeUtil() {}

    public static int todayIso() { return LocalDate.now().getDayOfWeek().getValue(); }

    public static String dayName(int iso) {
        return DayOfWeek.of(iso).getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    public static String dayShort(int iso) {
        return DayOfWeek.of(iso).getDisplayName(TextStyle.SHORT, Locale.getDefault());
    }

    public static String hm(int minutes) {
        return LocalTime.of((minutes / 60) % 24, minutes % 60)
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    public static long startOfDay(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static String formatDate(long millis) {
        return DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    public static String formatDateTime(long millis) {
        return DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.getDefault())
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    public static long daysUntil(long millis) {
        LocalDate target = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
        return ChronoUnit.DAYS.between(LocalDate.now(), target);
    }

    /** Earliest moment >= afterMillis that falls on isoDay at minuteOfDay (local time). */
    public static long nextWeekly(int isoDay, int minuteOfDay, long afterMillis) {
        ZoneId z = ZoneId.systemDefault();
        LocalDate d = Instant.ofEpochMilli(afterMillis).atZone(z).toLocalDate();
        for (int i = 0; i <= 7; i++) {
            LocalDate c = d.plusDays(i);
            if (c.getDayOfWeek().getValue() == isoDay) {
                long t = c.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(z).toInstant().toEpochMilli();
                if (t >= afterMillis) return t;
            }
        }
        return afterMillis + 7 * 86400000L;
    }
}
