package com.forge.memory.service;

import com.forge.common.util.SecurityUtils;
import com.forge.journal.entity.Journal;
import com.forge.journal.repository.JournalRepository;
import com.forge.memory.dto.MemoryResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final TopicRepository topicRepository;
    private final JournalRepository journalRepository;

    public MemoryResponse getMemory() {
        UUID userId = SecurityUtils.getCurrentUserId();
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        List<MemoryResponse.FadingConcept> fadingConcepts = computeFadingConcepts(allTopics);

        List<Journal> recentJournals = journalRepository
                .findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, thirtyDaysAgo, LocalDate.now());

        List<MemoryResponse.MemoryEntry> patterns = new ArrayList<>();
        List<MemoryResponse.MemoryEntry> mistakes = new ArrayList<>();
        List<MemoryResponse.MemoryEntry> insights = new ArrayList<>();

        for (Journal journal : recentJournals) {
            if (journal.getLessons() != null && !journal.getLessons().isBlank()) {
                String lower = journal.getLessons().toLowerCase();
                if (lower.contains("pattern") || lower.contains("template") || lower.contains("approach")) {
                    patterns.add(new MemoryResponse.MemoryEntry(
                            journal.getEntryDate(), journal.getLessons(), null, null));
                }
                insights.add(new MemoryResponse.MemoryEntry(
                        journal.getEntryDate(), journal.getLessons(), null, null));
            }
            if (journal.getChallenges() != null && !journal.getChallenges().isBlank()) {
                mistakes.add(new MemoryResponse.MemoryEntry(
                        journal.getEntryDate(), journal.getChallenges(), null, null));
            }
        }

        return new MemoryResponse(fadingConcepts, patterns, mistakes, insights);
    }

    private List<MemoryResponse.FadingConcept> computeFadingConcepts(List<Topic> topics) {
        List<MemoryResponse.FadingConcept> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Topic topic : topics) {
            long daysSinceRevision = -1;
            if (topic.getLastRevision() != null) {
                daysSinceRevision = Duration.between(topic.getLastRevision(), now).toDays();
            }

            boolean isFading = (topic.getNextRevision() != null && topic.getNextRevision().isBefore(now))
                    || (topic.getConfidence() < 4)
                    || (topic.getEstimatedRetention() != null && topic.getEstimatedRetention() < 60.0);

            if (isFading) {
                result.add(new MemoryResponse.FadingConcept(
                        topic.getId().toString(),
                        topic.getTitle(),
                        topic.getCategory(),
                        topic.getConfidence(),
                        topic.getMastery(),
                        daysSinceRevision,
                        topic.getEstimatedRetention()
                ));
            }
        }

        result.sort((a, b) -> {
            if (a.getConfidence() != b.getConfidence()) return a.getConfidence() - b.getConfidence();
            if (a.getDaysSinceRevision() > b.getDaysSinceRevision()) return -1;
            if (a.getDaysSinceRevision() < b.getDaysSinceRevision()) return 1;
            return 0;
        });

        return result;
    }
}
