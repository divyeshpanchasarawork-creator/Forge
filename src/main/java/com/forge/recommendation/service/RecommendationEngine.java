package com.forge.recommendation.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.repository.RecommendationRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final TopicRepository topicRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;

    public List<Recommendation> generateForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Recommendation> recs = new ArrayList<>();
        recs.addAll(checkLowConfidenceTopics(userId));
        recs.addAll(checkOverdueRevisions(userId));
        recs.addAll(checkNoActivityToday(userId));
        recs.addAll(checkMasteryThreshold(userId));

        recs.forEach(r -> r.setUser(user));

        return recs.stream()
                .sorted(Comparator.comparing(Recommendation::getPriority))
                .toList();
    }

    private List<Recommendation> checkLowConfidenceTopics(UUID userId) {
        List<Topic> weakTopics = topicRepository.findWeakTopicsByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();

        for (Topic topic : weakTopics) {
            Recommendation rec = new Recommendation();
            rec.setTitle("Review " + topic.getTitle());
            rec.setDescription("Your confidence in " + topic.getTitle() + " is only " + topic.getConfidence() + "/10.");
            rec.setReason("Low confidence indicates weak understanding. Regular review helps build mastery.");
            rec.setPriority(1);
            rec.setAction("REVIEW");
            rec.setDismissed(false);
            recs.add(rec);
        }

        return recs;
    }

    private List<Recommendation> checkOverdueRevisions(UUID userId) {
        List<Topic> overdueTopics = topicRepository.findTopicsNeedingRevisionByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();

        for (Topic topic : overdueTopics) {
            if (topic.getLastRevision() != null) {
                long daysSince = java.time.Duration.between(topic.getLastRevision(), LocalDateTime.now()).toDays();
                if (daysSince > 14) {
                    Recommendation rec = new Recommendation();
                    rec.setTitle(topic.getTitle() + " needs review");
                    rec.setDescription(topic.getTitle() + " hasn't been reviewed in " + daysSince + " days.");
                    rec.setReason("Spaced repetition requires regular review. Your retention drops without practice.");
                    rec.setPriority(1);
                    rec.setAction("REVIEW");
                    rec.setDismissed(false);
                    recs.add(rec);
                }
            }
        }

        return recs;
    }

    private List<Recommendation> checkNoActivityToday(UUID userId) {
        List<Recommendation> recs = new ArrayList<>();
        Recommendation rec = new Recommendation();
        rec.setTitle("Start your day with practice");
        rec.setDescription("You haven't logged any activity today.");
        rec.setReason("Consistency is key. Even 30 minutes of focused practice makes a difference.");
        rec.setPriority(2);
        rec.setAction("PRACTICE");
        rec.setDismissed(false);
        recs.add(rec);
        return recs;
    }

    private List<Recommendation> checkMasteryThreshold(UUID userId) {
        List<Topic> strongTopics = topicRepository.findStrongTopicsByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();

        for (Topic topic : strongTopics) {
            if (topic.getMastery() > 80) {
                Recommendation rec = new Recommendation();
                rec.setTitle("Ready for advanced " + topic.getTitle());
                rec.setDescription("Great progress on " + topic.getTitle() + "! Mastery: " + topic.getMastery() + "%");
                rec.setReason("High mastery means you're ready to tackle more complex concepts in this area.");
                rec.setPriority(3);
                rec.setAction("ADVANCE");
                rec.setDismissed(false);
                recs.add(rec);
            }
        }

        return recs;
    }
}
