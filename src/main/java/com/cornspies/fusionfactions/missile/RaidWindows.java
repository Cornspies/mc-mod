package com.cornspies.fusionfactions.missile;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.cornspies.fusionfactions.config.MissileConfig;

// Raid windows (D46): the times at which missiles can be launched, from the config. Each entry is
// "<days> <HH:MM>-<HH:MM>", e.g. "SAT 18:00-23:00", "MON-FRI 20:00-22:00", "SAT,SUN 14:00-02:00",
// "DAILY 20:00-22:00". Days: MON … SUN, ranges like MON-FRI, lists with commas, DAILY, WEEKDAYS,
// WEEKEND. An end before the start runs past midnight; equal start and end is the whole day.
// No entries means missiles can always be launched.
public final class RaidWindows {
    public record Window(Set<DayOfWeek> days, LocalTime start, LocalTime end) {
        boolean contains(ZonedDateTime time) {
            LocalTime clock = time.toLocalTime();
            DayOfWeek day = time.getDayOfWeek();
            if (!this.end.isAfter(this.start)) {
                // Runs past midnight (or all day): open from the start on its days until the end on the next day.
                return (this.days.contains(day) && !clock.isBefore(this.start))
                        || (this.days.contains(day.minus(1)) && clock.isBefore(this.end));
            }
            return this.days.contains(day) && !clock.isBefore(this.start) && clock.isBefore(this.end);
        }
    }

    // Parses one config entry, or returns null if it's not valid.
    public static @Nullable Window parse(String entry) {
        String[] parts = entry.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (parts.length != 2) {
            return null;
        }
        Set<DayOfWeek> days = parseDays(parts[0]);
        String[] times = parts[1].split("-");
        if (days == null || times.length != 2) {
            return null;
        }
        try {
            return new Window(days, LocalTime.parse(times[0]), LocalTime.parse(times[1]));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static @Nullable Set<DayOfWeek> parseDays(String text) {
        switch (text) {
            case "DAILY":
                return EnumSet.allOf(DayOfWeek.class);
            case "WEEKDAYS":
                return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            case "WEEKEND":
            case "WEEKENDS":
                return EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            default:
                break;
        }
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String part : text.split(",")) {
            String[] range = part.split("-");
            DayOfWeek first = day(range[0]);
            DayOfWeek last = range.length == 2 ? day(range[1]) : first;
            if (range.length > 2 || first == null || last == null) {
                return null;
            }
            // Ranges may wrap around the week, e.g. FRI-MON.
            for (DayOfWeek day = first; ; day = day.plus(1)) {
                days.add(day);
                if (day == last) {
                    break;
                }
            }
        }
        return days;
    }

    private static @Nullable DayOfWeek day(String text) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().startsWith(text) && text.length() >= 3) {
                return day;
            }
        }
        return null;
    }

    public static boolean isValid(Object entry) {
        return entry instanceof String text && parse(text) != null;
    }

    public static List<Window> configured() {
        List<Window> windows = new ArrayList<>();
        for (String entry : MissileConfig.raidWindows()) {
            Window window = parse(entry);
            if (window != null) {
                windows.add(window);
            }
        }
        return windows;
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(MissileConfig.raidTimeZone());
    }

    public static boolean isOpen(List<Window> windows, ZonedDateTime time) {
        return windows.isEmpty() || windows.stream().anyMatch(window -> window.contains(time));
    }

    // Time until the next window opens, or empty if one is open now.
    public static Optional<Duration> untilOpen(List<Window> windows, ZonedDateTime time) {
        if (isOpen(windows, time)) {
            return Optional.empty();
        }
        ZonedDateTime next = null;
        for (Window window : windows) {
            for (int days = 0; days <= 7; days++) {
                ZonedDateTime start = time.truncatedTo(ChronoUnit.DAYS).plusDays(days).with(window.start());
                if (window.days().contains(start.getDayOfWeek()) && start.isAfter(time) && (next == null || start.isBefore(next))) {
                    next = start;
                }
            }
        }
        return Optional.of(next == null ? Duration.ZERO : Duration.between(time, next));
    }

    public static boolean isValidZone(Object zone) {
        if (!(zone instanceof String text)) {
            return false;
        }
        if (text.isEmpty()) {
            return true;
        }
        try {
            ZoneId.of(text);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private RaidWindows() {
    }
}
