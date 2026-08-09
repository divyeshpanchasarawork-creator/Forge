package com.forge.common.util;

import com.forge.auth.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class TimezoneUtil {

    private static final ZoneId FALLBACK = ZoneId.of("UTC");

    private TimezoneUtil() {
    }

    public static ZoneId resolve(User user) {
        if (user == null || user.getTimezone() == null) {
            return FALLBACK;
        }
        try {
            return ZoneId.of(user.getTimezone());
        } catch (Exception e) {
            return FALLBACK;
        }
    }

    /**
     * Current wall-clock time in the user's timezone. All business timestamps
     * (attempts, revisions, mastery) are written and read in this zone so that
     * day-boundary logic stays self-consistent regardless of the server zone.
     */
    public static LocalDateTime now(User user) {
        return LocalDateTime.now(resolve(user));
    }

    public static LocalDate today(User user) {
        return LocalDate.now(resolve(user));
    }

    public static LocalDateTime dayStart(User user) {
        return today(user).atStartOfDay();
    }

    public static LocalDateTime dayEnd(User user) {
        return today(user).atTime(LocalTime.MAX);
    }
}
