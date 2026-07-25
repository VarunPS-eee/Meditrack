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

/**
 * Date and time helpers built on {@code java.time}.
 *
 * <p>The project uses {@link LocalDateTime} rather than the legacy {@link java.util.Date}
 * throughout. {@code Date} is mutable — every getter handing one out needs a defensive
 * copy, and forgetting one is a silent aliasing bug. {@code LocalDateTime} is immutable,
 * so that entire class of defect cannot occur.</p>
 *
 * <p>The {@link DateTimeFormatter} constants are {@code static final} because, unlike the
 * old {@code SimpleDateFormat}, they are <b>immutable and thread-safe</b> — one shared
 * instance is correct even with the reminder thread running.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
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

    // ------------------------------------------------------------------ format
    /**
     * @param dateTime the value to format, may be {@code null}
     * @return the storage-format string, or {@code "N/A"} for {@code null}
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * @param dateTime the value to format, may be {@code null}
     * @return the display-format string, or {@code "N/A"} for {@code null}
     */
    public static String formatForDisplay(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * @param date the value to format, may be {@code null}
     * @return {@code yyyy-MM-dd}, or {@code "N/A"} for {@code null}
     */
    public static String formatDate(LocalDate date) {
        return date == null ? "N/A" : date.format(DATE_FORMATTER);
    }

    // ------------------------------------------------------------------- parse
    /**
     * Parses a {@code yyyy-MM-dd HH:mm} timestamp.
     *
     * <p>Wraps {@link DateTimeParseException} in an {@link InvalidDataException} —
     * <b>exception chaining</b>: the caller gets a domain-level message while the
     * original parse failure survives as {@link Throwable#getCause()}.</p>
     *
     * @param text the text to parse
     * @return the parsed timestamp
     * @throws InvalidDataException if the text is null, blank or malformed
     */
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

    /**
     * Parses a {@code yyyy-MM-dd} date.
     *
     * @param text the text to parse
     * @return the parsed date
     * @throws InvalidDataException if the text is null, blank or malformed
     */
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

    /**
     * Lenient parse used when reloading persisted data: returns {@code null} instead of
     * throwing, so one corrupt row does not abort the whole import.
     *
     * @param text the text to parse
     * @return the parsed timestamp, or {@code null} if unparseable
     */
    public static LocalDateTime parseDateTimeOrNull(String text) {
        try {
            return parseDateTime(text);
        } catch (InvalidDataException e) {
            return null;
        }
    }

    // ------------------------------------------------------------- clinic rules
    /**
     * @param slot the slot to test
     * @return {@code true} if the slot falls inside clinic opening hours
     */
    public static boolean isWithinClinicHours(LocalDateTime slot) {
        if (slot == null) {
            return false;
        }
        int hour = slot.getHour();
        return hour >= Constants.CLINIC_OPEN_HOUR && hour < Constants.CLINIC_CLOSE_HOUR;
    }

    /**
     * @param slot the slot to test
     * @return {@code true} if the slot is on a Saturday or Sunday
     */
    public static boolean isWeekend(LocalDateTime slot) {
        if (slot == null) {
            return false;
        }
        return switch (slot.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    /**
     * @param slot the slot to test
     * @return {@code true} if the slot is strictly in the future
     */
    public static boolean isFuture(LocalDateTime slot) {
        return slot != null && slot.isAfter(LocalDateTime.now());
    }

    /**
     * Rounds a timestamp down to the nearest clinic slot boundary.
     *
     * @param dateTime the timestamp to align
     * @return the aligned timestamp, seconds and nanos cleared
     */
    public static LocalDateTime alignToSlot(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        int minute = (dateTime.getMinute() / Constants.SLOT_DURATION_MINUTES)
                * Constants.SLOT_DURATION_MINUTES;
        return dateTime.withMinute(minute).withSecond(0).withNano(0);
    }

    /**
     * Generates every bookable slot on a given day, in order.
     *
     * @param date the day to enumerate
     * @return the day's slots between opening and closing time
     */
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

    // --------------------------------------------------------------- durations
    /**
     * @param from earlier timestamp
     * @param to   later timestamp
     * @return whole hours between the two, or {@code 0} if either is {@code null}
     */
    public static long hoursBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(from, to);
    }

    /**
     * @param from earlier timestamp
     * @param to   later timestamp
     * @return whole days between the two, or {@code 0} if either is {@code null}
     */
    public static long daysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(from, to);
    }

    /**
     * Renders the gap to a future slot in words, for reminder messages.
     *
     * @param slot the upcoming slot
     * @return e.g. {@code "in 2 hours"}, {@code "in 3 days"} or {@code "now"}
     */
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
