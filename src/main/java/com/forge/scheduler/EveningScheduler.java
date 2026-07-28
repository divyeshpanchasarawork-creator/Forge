package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EveningScheduler {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata")
    public void eveningUpdate() {
        log.info("Running evening scheduler...");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<Topic> allTopics = topicRepository.findByUserId(user.getId(), PageRequest.of(0, 1000)).getContent();
            for (Topic topic : allTopics) {
                if (topic.getLastRevision() != null) {
                    long daysSinceRevision = java.time.Duration.between(topic.getLastRevision(), LocalDateTime.now()).toDays();
                    double retention = 100 * Math.exp(-0.07 * daysSinceRevision);
                    topic.setEstimatedRetention(Math.max(0, retention));
                }
            }
            topicRepository.saveAll(allTopics);
            log.info("Updated {} topics for user {}", allTopics.size(), user.getUsername());
        }
        log.info("Evening scheduler completed for {} users.", users.size());
    }
}
