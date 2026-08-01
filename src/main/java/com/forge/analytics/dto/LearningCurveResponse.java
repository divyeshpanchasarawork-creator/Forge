package com.forge.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningCurveResponse {

    private List<CurvePoint> points;
    private List<Milestone> milestones;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurvePoint {
        private String date;
        private double mastery;
        private double confidence;
        private double retention;
        private double skillRating;
        private double consistency;
        private int solved;
        private int revisions;
        private List<String> milestones;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Milestone {
        private String date;
        private String type;
        private String label;
    }
}
