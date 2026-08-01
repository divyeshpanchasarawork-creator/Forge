package com.forge.scheduler;

import com.forge.analytics.service.MetricSnapshotService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EveningScheduler {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ForgettingCurveService forgettingCurveService;
    private final MetricSnapshotService metricSnapshotService;

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata")
    public void eveningUpdate() {
        log.info("Running evening scheduler...");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                forgettingCurveService.refreshUserRetentions(user.getId());
                metricSnapshotService.snapshotForUser(user.getId());
                log.info("Evening update complete for user {}", user.getUsername());
            } catch (Exception e) {
                log.warn("Evening update failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("Evening scheduler completed for {} users.", users.size());
    }
}
