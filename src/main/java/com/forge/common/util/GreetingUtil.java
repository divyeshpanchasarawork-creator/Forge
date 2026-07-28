package com.forge.common.util;

public final class GreetingUtil {

    private GreetingUtil() {
    }

    public static String getGreeting(String displayName) {
        int hour = java.time.LocalTime.now().getHour();
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
