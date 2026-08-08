package com.forge.intelligence.service;

import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgettingCurveService {

    private static final double BASE_DECAY_RATE = 0.07;
    private static final double MIN_STRENGTH = 0.4;
    private static final double MAX_STRENGTH = 4.0;

    private final TopicRepository topicRepository;

    public double strengthFor(Topic topic) {
        double strength = topic.getMemoryStrength() != null ? topic.getMemoryStrength() : 1.0;
        if (topic.getEasinessFactor() != null && topic.getEasinessFactor() > 0) {
            strength = Math.min(MAX_STRENGTH, Math.max(MIN_STRENGTH, strength * (2.5 / topic.getEasinessFactor())));
        }
        return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength));
    }

    public double decayRate(Topic topic) {
        return BASE_DECAY_RATE / Math.max(MIN_STRENGTH, strengthFor(topic));
    }

    public double computeRetention(Topic topic, LocalDateTime asOf) {
        LocalDateTime anchor = topic.getLastRevision() != null ? topic.getLastRevision() : topic.getLastAttemptAt();
        if (anchor == null) {
            return 100.0;
        }
        long days = Duration.between(anchor, asOf).toDays();
        if (days <= 0) return 100.0;
        double retention = 100 * Math.exp(-decayRate(topic) * days);
        return Math.max(0, Math.min(100, retention));
    }

    public void refreshTopicRetention(Topic topic) {
        topic.setEstimatedRetention(computeRetention(topic, LocalDateTime.now()));
    }

    public void refreshUserRetentions(UUID userId) {
        List<Topic> topics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000));
        for (Topic topic : topics) {
            refreshTopicRetention(topic);
        }
        topicRepository.saveAll(topics);
    }

    public double strengthen(Topic topic, double solvedFraction) {
        double strength = topic.getMemoryStrength() != null ? topic.getMemoryStrength() : 1.0;
        strength = strength + 0.08 + solvedFraction * 0.12;
        topic.setMemoryStrength(Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength)));
        return topic.getMemoryStrength();
    }
}
