package com.forge.revision.service;

import com.forge.topic.entity.Topic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacedRepetitionServiceTest {

    private final SpacedRepetitionService service = new SpacedRepetitionService();

    private Topic topic(int revisionCount, Integer interval, Double ef) {
        Topic topic = new Topic();
        topic.setRevisionCount(revisionCount);
        topic.setRepetitionInterval(interval);
        topic.setEasinessFactor(ef);
        return topic;
    }

    @Test
    void firstSuccessSchedulesOneDay() {
        SpacedRepetitionService.Sm2Result result = service.calculate(topic(0, null, null), 4);

        assertEquals(1, result.intervalDays());
        assertEquals(2.5, result.easinessFactor());
        assertTrue(result.masteryBoost() > 0);
    }

    @Test
    void secondSuccessSchedulesSixDays() {
        SpacedRepetitionService.Sm2Result result = service.calculate(topic(1, 1, 2.5), 4);

        assertEquals(6, result.intervalDays());
    }

    @Test
    void laterSuccessesGrowTheIntervalByTheEasinessFactor() {
        SpacedRepetitionService.Sm2Result result = service.calculate(topic(2, 6, 2.5), 4);

        assertEquals(15, result.intervalDays());
    }

    @Test
    void lowQualityResetsToOneDayWithNoBoost() {
        SpacedRepetitionService.Sm2Result result = service.calculate(topic(2, 6, 2.5), 2);

        assertEquals(1, result.intervalDays());
        assertEquals(0, result.masteryBoost());
    }

    @Test
    void easinessFactorNeverDropsBelowFloor() {
        SpacedRepetitionService.Sm2Result result = service.calculate(topic(0, null, 1.4), 0);

        assertEquals(1.3, result.easinessFactor());
    }

    @Test
    void masteryBoostIsCappedAtTenAndNeverNegative() {
        SpacedRepetitionService.Sm2Result high = service.calculate(topic(0, null, 2.5), 5);
        SpacedRepetitionService.Sm2Result low = service.calculate(topic(10, 6, 1.3), 0);

        assertEquals(10, high.masteryBoost());
        assertEquals(0, low.masteryBoost());
    }

    @Test
    void qualityIsClampedToValidRange() {
        SpacedRepetitionService.Sm2Result over = service.calculate(topic(0, null, 2.5), 9);
        SpacedRepetitionService.Sm2Result high = service.calculate(topic(0, null, 2.5), 5);
        SpacedRepetitionService.Sm2Result under = service.calculate(topic(0, null, 2.5), -3);
        SpacedRepetitionService.Sm2Result low = service.calculate(topic(0, null, 2.5), 0);

        assertEquals(high.intervalDays(), over.intervalDays());
        assertEquals(high.easinessFactor(), over.easinessFactor());
        assertEquals(high.masteryBoost(), over.masteryBoost());
        assertEquals(low.intervalDays(), under.intervalDays());
        assertEquals(low.easinessFactor(), under.easinessFactor());
        assertEquals(low.masteryBoost(), under.masteryBoost());
    }
}
