package com.forge.analytics.dto;

import java.time.LocalDate;

public record ActivityDay(
        LocalDate date,
        boolean active,
        double hours,
        int attempts,
        int revisions
) {
}
