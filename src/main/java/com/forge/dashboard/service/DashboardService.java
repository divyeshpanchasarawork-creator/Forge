package com.forge.dashboard.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.GreetingUtil;
import com.forge.common.util.SecurityUtils;
import com.forge.dashboard.dto.DashboardResponse;
import com.forge.journal.entity.Journal;
import com.forge.journal.repository.JournalRepository;
import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.service.RecommendationService;
import com.forge.revision.dto.RevisionResponse;
import com.forge.revision.service.RevisionService;
import com.forge.topic.dto.TopicResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import com.forge.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final TopicService topicService;
    private final RevisionService revisionService;
    private final RecommendationService recommendationService;
    private final JournalRepository journalRepository;
    private final TopicRepository topicRepository;

    public DashboardResponse getDashboard() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String greeting = GreetingUtil.getGreeting(user.getDisplayName());

        List<TopicResponse> weakTopics = topicService.getWeakTopics();
        String currentFocus = weakTopics.isEmpty() ? "All topics are well covered!" : weakTopics.getFirst().getTitle();

        List<RevisionResponse> revisions = revisionService.getTodayRevisions();
        long revisionsDue = revisions.size();

        String todayMission = String.format("Review %d topics, maintain your streak!", revisionsDue);

        List<RecommendationResponse> recommendations = recommendationService.getActiveRecommendations();

        List<TopicResponse> strongTopics = topicService.getStrongTopics();

        List<Topic> allTopics = topicRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
        int avgMastery = allTopics.isEmpty() ? 0 : (int) allTopics.stream().mapToInt(Topic::getMastery).average().orElse(0);
        int avgConfidence = allTopics.isEmpty() ? 0 : (int) allTopics.stream().mapToInt(Topic::getConfidence).average().orElse(0);
        long masteredCount = allTopics.stream().filter(t -> "MASTERED".equals(t.getStatus())).count();

        var todayJournal = journalRepository.findByUserIdAndEntryDate(userId, LocalDate.now()).orElse(null);
        String journalSummary = todayJournal != null ?
                "Energy: " + todayJournal.getEnergy() + "/5, Mood: " + todayJournal.getMood() + "/5" :
                "No journal entry today yet.";

        return new DashboardResponse(
                greeting,
                currentFocus,
                todayMission,
                revisions,
                recommendations,
                weakTopics,
                strongTopics,
                new DashboardResponse.KnowledgeHealth(avgMastery, avgConfidence, allTopics.size(), masteredCount),
                new DashboardResponse.WeeklyProgress(0, 0, 0.0, 0),
                journalSummary,
                List.of()
        );
    }
}
