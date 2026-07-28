package com.forge.journal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JournalRequest {

    private LocalDate entryDate;

    private String morningGoal;

    private String eveningReflection;

    @Min(1) @Max(5)
    private Integer energy;

    @Min(1) @Max(5)
    private Integer mood;

    private Double hoursStudied;

    private String achievements;

    private String challenges;

    private String lessons;
}
