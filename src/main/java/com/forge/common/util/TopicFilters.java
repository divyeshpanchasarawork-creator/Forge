package com.forge.common.util;

import com.forge.topic.entity.Topic;

public final class TopicFilters {

    private TopicFilters() {
    }

    public static boolean isUnengagedColdStart(Topic topic) {
        return "COLD_START".equals(topic.getSource())
                && (topic.getAttemptsTotal() == null || topic.getAttemptsTotal() == 0);
    }

    public static boolean isEngaged(Topic topic) {
        return !isUnengagedColdStart(topic);
    }
}
