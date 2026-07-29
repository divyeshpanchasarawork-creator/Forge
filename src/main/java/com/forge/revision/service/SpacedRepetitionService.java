package com.forge.revision.service;

import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Component;

@Component
public class SpacedRepetitionService {

    static final double MIN_EF = 1.3;
    static final double INITIAL_EF = 2.5;

    public record Sm2Result(
            int intervalDays,
            double easinessFactor,
            int masteryBoost
    ) {}

    public Sm2Result calculate(Topic topic, int quality) {
        int clampedQuality = Math.max(0, Math.min(5, quality));
        double ef = topic.getEasinessFactor() != null ? topic.getEasinessFactor() : INITIAL_EF;
        int interval = topic.getRepetitionInterval() != null ? topic.getRepetitionInterval() : 0;
        int revisionCount = topic.getRevisionCount() != null ? topic.getRevisionCount() : 0;

        int newInterval;
        int masteryBoost;

        if (clampedQuality < 3) {
            newInterval = 1;
            masteryBoost = 0;
        } else {
            if (revisionCount == 0) {
                newInterval = 1;
            } else if (revisionCount == 1) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.round(interval * ef);
            }
            masteryBoost = (int) Math.min(10, 3 + (quality * 1.5) - (revisionCount * 0.3));
            masteryBoost = Math.max(0, masteryBoost);
        }

        ef = ef + (0.1 - (5 - clampedQuality) * (0.08 + (5 - clampedQuality) * 0.02));
        ef = Math.max(MIN_EF, ef);

        return new Sm2Result(newInterval, ef, masteryBoost);
    }
}
