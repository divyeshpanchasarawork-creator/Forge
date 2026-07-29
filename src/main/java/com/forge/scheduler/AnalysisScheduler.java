package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.recommendation.service.RecommendationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final UserRepository userRepository;
    private final RecommendationEngine recommendationEngine;

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void runScheduledAnalyses() {
        LocalTime now = LocalTime.now();
        LocalTime windowStart = now.minusMinutes(15);
        LocalTime windowEnd = now.plusMinutes(15);
        LocalDate today = LocalDate.now();

        List<User> candidates = userRepository.findByPreferredAnalysisTimeIsNotNull();
        int processed = 0;

        for (User user : candidates) {
            LocalTime preferred = user.getPreferredAnalysisTime();
            if (preferred == null) continue;
            if (preferred.isBefore(windowStart) || preferred.isAfter(windowEnd)) continue;

            if (user.getLastGenerationDate() == null || !user.getLastGenerationDate().equals(today)) {
                user.setDailyGenerationsUsed(0);
                user.setLastGenerationDate(today);
            }
            if (user.getDailyGenerationsUsed() >= 4) continue;

            try {
                recommendationEngine.generateForUser(user.getId(), true);
                user.setDailyGenerationsUsed(user.getDailyGenerationsUsed() + 1);
                userRepository.save(user);
                processed++;
            } catch (Exception e) {
                log.error("Scheduled generation failed for user {}: {}", user.getId(), e.getMessage());
            }
        }

        if (processed > 0) {
            log.info("AnalysisScheduler: ran scheduled generation for {} user(s)", processed);
        }
    }
}
