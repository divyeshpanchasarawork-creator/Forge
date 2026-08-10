package com.forge.intelligence.service;

import com.forge.auth.entity.User;
import com.forge.topic.entity.Topic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasteryServiceTest {

    private final MasteryService service = new MasteryService();

    @Test
    void solvedWithoutHintsGetsFullQuality() {
        assertEquals(5, service.qualityFrom("SOLVED", 0, null));
    }

    @Test
    void solvedPenaltyScalesWithHintsInsteadOfSaturating() {
        assertEquals(4, service.qualityFrom("SOLVED", 1, null));
        assertEquals(3, service.qualityFrom("SOLVED", 2, null));
        assertEquals(2, service.qualityFrom("SOLVED", 3, null));
        assertEquals(1, service.qualityFrom("SOLVED", 4, null));
    }

    @Test
    void outcomeOrderingIsPreserved() {
        int solvedWithTwoHints = service.qualityFrom("SOLVED", 2, null);
        int partial = service.qualityFrom("PARTIAL", 0, null);
        int failed = service.qualityFrom("FAILED", 0, null);
        int skipped = service.qualityFrom("SKIPPED", 0, null);

        assertEquals(3, solvedWithTwoHints);
        assertEquals(3, partial);
        assertEquals(1, failed);
        assertEquals(0, skipped);
    }

    @Test
    void slowSolvesArePenalizedButNeverBelowZero() {
        assertEquals(4, service.qualityFrom("SOLVED", 0, 3000));
        assertEquals(0, service.qualityFrom("SKIPPED", 0, 3000));
    }

    @Test
    void applyStoresTheHintAwareQuality() {
        Topic topic = new Topic();
        User user = new User();
        user.setTimezone("UTC");
        topic.setUser(user);

        service.apply(topic, "SOLVED", 3, null);

        assertEquals(2, topic.getLastQuality());
    }
}
