package com.forge.scheduler;

import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.analytics.service.AnalyticsService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyScheduler {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 20 * * 0", zone = "Asia/Kolkata")
    public void weeklyReport() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                WeeklyProgressResponse weeklyProgress = analyticsService.getWeeklyProgress(user.getId());
                log.info("Weekly report for {}: {} revisions completed, {} journal entries",
                        user.getUsername(), weeklyProgress.getRevisionsCompleted(), weeklyProgress.getJournalEntries());
            } catch (Exception e) {
                log.warn("Could not generate weekly report for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
