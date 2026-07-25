package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.exception.InvalidDataException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class DateUtil {

    /** {@code yyyy-MM-dd} — the storage format used in CSV. */
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);

    /** {@code yyyy-MM-dd HH:mm} — the storage format for timestamps. */
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT);

    /** {@code dd MMM yyyy, hh:mm a} — the friendlier console format. */
    public static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DISPLAY_DATE_TIME_FORMAT);

    private DateUtil() {
        throw new AssertionError("DateUtil is a utility class and must not be instantiated.");
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatForDisplay(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.format(DISPLAY_FORMATTER);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "N/A" : date.format(DATE_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String text) throws InvalidDataException {
        if (text == null || text.isBlank()) {
            throw new InvalidDataException("dateTime", text, "date/time is required");
        }
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("dateTime", text,
                    "expected format " + Constants.DATE_TIME_FORMAT, e);
        }
    }

    public static LocalDate parseDate(String text) throws InvalidDataException {
        if (text == null || text.isBlank()) {
            throw new InvalidDataException("date", text, "date is required");
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidDataException("date", text,
                    "expected format " + Constants.DATE_FORMAT, e);
        }
    }

    public static LocalDateTime parseDateTimeOrNull(String text) {
        try {
            return parseDateTime(text);
        } catch (InvalidDataException e) {
            return null;
        }
    }

    public static boolean isWithinClinicHours(LocalDateTime slot) {
        if (slot == null) {
            return false;
        }
        int hour = slot.getHour();
        return hour >= Constants.CLINIC_OPEN_HOUR && hour < Constants.CLINIC_CLOSE_HOUR;
    }

    public static boolean isWeekend(LocalDateTime slot) {
        if (slot == null) {
            return false;
        }
        return switch (slot.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    public static boolean isFuture(LocalDateTime slot) {
        return slot != null && slot.isAfter(LocalDateTime.now());
    }

    public static LocalDateTime alignToSlot(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        int minute = (dateTime.getMinute() / Constants.SLOT_DURATION_MINUTES)
                * Constants.SLOT_DURATION_MINUTES;
        return dateTime.withMinute(minute).withSecond(0).withNano(0);
    }

    public static List<LocalDateTime> generateSlotsForDay(LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();
        if (date == null) {
            return slots;
        }
        LocalDateTime cursor = LocalDateTime.of(date, LocalTime.of(Constants.CLINIC_OPEN_HOUR, 0));
        LocalDateTime close = LocalDateTime.of(date, LocalTime.of(Constants.CLINIC_CLOSE_HOUR, 0));
        while (cursor.isBefore(close)) {
            slots.add(cursor);
            cursor = cursor.plusMinutes(Constants.SLOT_DURATION_MINUTES);
        }
        return slots;
    }

    public static long hoursBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(from, to);
    }

    public static long daysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(from, to);
    }

    public static String describeTimeUntil(LocalDateTime slot) {
        if (slot == null) {
            return "unknown";
        }
        Duration gap = Duration.between(LocalDateTime.now(), slot);
        if (gap.isNegative()) {
            return "in the past";
        }
        long days = gap.toDays();
        if (days > 0) {
            return "in " + days + (days == 1 ? " day" : " days");
        }
        long hours = gap.toHours();
        if (hours > 0) {
            return "in " + hours + (hours == 1 ? " hour" : " hours");
        }
        long minutes = gap.toMinutes();
        return minutes > 0 ? "in " + minutes + " minutes" : "now";
    }

    /** @return the current timestamp, aligned to a slot boundary */
    public static LocalDateTime nowAlignedToSlot() {
        return alignToSlot(LocalDateTime.now());
    }
}
