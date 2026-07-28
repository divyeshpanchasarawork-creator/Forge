package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.analytics.service.AnalyticsService;
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

    @Scheduled(cron = "0 0 20 * * 0", zone = "Asia/Kolkata")
    public void weeklyReport() {
        log.info("Generating weekly report...");
        try {
            var weeklyProgress = analyticsService.getWeeklyProgress();
            log.info("Weekly report: {} revisions completed, {} journal entries",
                    weeklyProgress.getRevisionsCompleted(), weeklyProgress.getJournalEntries());
        } catch (Exception e) {
            log.warn("Could not generate weekly report (likely no authenticated user): {}", e.getMessage());
        }
    }
}
