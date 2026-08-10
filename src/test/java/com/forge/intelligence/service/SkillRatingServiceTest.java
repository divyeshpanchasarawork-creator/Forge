package com.forge.intelligence.service;

import com.forge.topic.entity.Topic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillRatingServiceTest {

    @Test
    void shouldReturnInitialRatingForNullTopics() {
        assertEquals(SkillRatingService.INITIAL_RATING, SkillRatingService.skillFromTopics(null), 0.0001);
    }

    @Test
    void shouldReturnInitialRatingForEmptyTopics() {
        assertEquals(SkillRatingService.INITIAL_RATING, SkillRatingService.skillFromTopics(List.of()), 0.0001);
    }

    @Test
    void shouldReturnInitialRatingWhenNoAttemptsExist() {
        Topic t1 = topic(1100.0, 0);
        Topic t2 = topic(1400.0, 0);

        assertEquals(SkillRatingService.INITIAL_RATING, SkillRatingService.skillFromTopics(List.of(t1, t2)), 0.0001);
    }

    @Test
    void shouldComputeAttemptWeightedAverage() {
        Topic t1 = topic(1200.0, 3);
        Topic t2 = topic(2000.0, 1);

        double expected = (1200.0 * 3 + 2000.0 * 1) / 4.0;
        assertEquals(expected, SkillRatingService.skillFromTopics(List.of(t1, t2)), 0.0001);
    }

    @Test
    void shouldIgnoreTopicsWithoutSkillRating() {
        Topic t1 = topic(1200.0, 3);
        Topic t2 = topic(null, 1);

        assertEquals(1200.0, SkillRatingService.skillFromTopics(List.of(t1, t2)), 0.0001);
    }

    @Test
    void shouldReturnInitialRatingWhenNoTopicHasSkillRating() {
        Topic t1 = topic(null, 3);
        Topic t2 = topic(null, 7);

        assertEquals(SkillRatingService.INITIAL_RATING, SkillRatingService.skillFromTopics(List.of(t1, t2)), 0.0001);
    }

    private Topic topic(Double skillRating, int attemptsTotal) {
        Topic t = new Topic();
        t.setSkillRating(skillRating);
        t.setAttemptsTotal(attemptsTotal);
        return t;
    }
}
