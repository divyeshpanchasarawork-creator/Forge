package com.forge.journal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JournalRequest {

    private LocalDate entryDate;

    @Size(max = 2000)
    private String morningGoal;

    @Size(max = 2000)
    private String eveningReflection;

    @Min(1) @Max(5)
    private Integer energy;

    @Min(1) @Max(5)
    private Integer mood;

    @Min(0) @Max(24)
    private Double hoursStudied;

    @Size(max = 2000)
    private String achievements;

    @Size(max = 2000)
    private String challenges;

    @Size(max = 2000)
    private String lessons;
}
