package com.forge.common.util;

import java.time.LocalTime;
import java.time.ZoneId;

public final class GreetingUtil {

    private GreetingUtil() {
    }

    public static String getGreeting(String displayName, ZoneId zone) {
        int hour = LocalTime.now(zone != null ? zone : ZoneId.systemDefault()).getHour();
        String timeOfDay;
        if (hour < 12) {
            timeOfDay = "Good Morning";
        } else if (hour < 17) {
            timeOfDay = "Good Afternoon";
        } else {
            timeOfDay = "Good Evening";
        }
        return timeOfDay + ", " + displayName;
    }
}
