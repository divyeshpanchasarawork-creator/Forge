package com.forge.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalResponse {

    private UUID id;
    private LocalDate entryDate;
    private String morningGoal;
    private String eveningReflection;
    private Integer energy;
    private Integer mood;
    private Double hoursStudied;
    private String achievements;
    private String challenges;
    private String lessons;
}
