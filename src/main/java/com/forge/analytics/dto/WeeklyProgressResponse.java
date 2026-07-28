package com.forge.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyProgressResponse {

    private long problemsSolved;
    private long topicsReviewed;
    private Double hoursStudied;
    private long revisionsCompleted;
    private long journalEntries;
}
