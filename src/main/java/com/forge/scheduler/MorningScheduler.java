package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.recommendation.service.RecommendationEngine;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MorningScheduler {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final RecommendationEngine recommendationEngine;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void generateDailyMission() {
        log.info("Running morning scheduler...");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<Topic> needingRevision = topicRepository.findTopicsNeedingRevisionByUserId(user.getId());
            log.info("User {} has {} topics needing revision", user.getUsername(), needingRevision.size());
            recommendationEngine.generateForUser(user.getId(), true);
        }
        log.info("Morning scheduler completed for {} users.", users.size());
    }
}
