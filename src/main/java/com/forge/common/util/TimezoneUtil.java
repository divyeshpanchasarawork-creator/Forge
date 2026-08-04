package com.forge.common.util;

import com.forge.auth.entity.User;

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
}
