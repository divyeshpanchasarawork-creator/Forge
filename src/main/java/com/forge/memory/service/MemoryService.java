package com.forge.memory.service;

import com.forge.auth.repository.UserRepository;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TimezoneUtil;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.memory.dto.MemoryResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final TopicRepository topicRepository;
    private final LeetCodeTagStatRepository tagStatRepository;
    private final ProblemLoader problemLoader;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MemoryResponse getMemory() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000));
        List<MemoryResponse.FadingConcept> fadingConcepts = computeFadingConcepts(allTopics, userId);
        return new MemoryResponse(fadingConcepts);
    }

    private List<MemoryResponse.FadingConcept> computeFadingConcepts(List<Topic> topics, UUID userId) {
        List<MemoryResponse.FadingConcept> result = new ArrayList<>();
        LocalDateTime now = TimezoneUtil.now(userRepository.findById(userId).orElse(null));

        for (Topic topic : topics) {
            // Never-attempted cold-start topics (e.g. from LeetCode sync) aren't "fading" —
            // mirror TopicRepository.findWeakTopicsByUserId / TopicFilters.isEngaged.
            if (!com.forge.common.util.TopicFilters.isEngaged(topic)) {
                continue;
            }

            long daysSinceRevision = -1;
            if (topic.getLastRevision() != null) {
                daysSinceRevision = Duration.between(topic.getLastRevision(), now).toDays();
            }

            boolean isFading = (topic.getNextRevision() != null && topic.getNextRevision().isBefore(now.toLocalDate()))
                    || (topic.getConfidence() < 4)
                    || (topic.getEstimatedRetention() != null && topic.getEstimatedRetention() < 60.0);

            if (isFading) {
                String suggestedTitle = null;
                String suggestedSlug = null;
                String suggestedDifficulty = null;

                String topicSlug = topic.getTitle().toLowerCase().replace(' ', '-').replaceAll("[^a-z0-9-]", "");
                List<ProblemLoader.ProblemEntry> candidates = problemLoader.getProblemsForTag(topicSlug);
                if (candidates.isEmpty()) {
                    candidates = problemLoader.getProblemsForTag(topic.getTitle().toLowerCase());
                }
                if (!candidates.isEmpty()) {
                    ProblemLoader.ProblemEntry pick = candidates.get(0);
                    suggestedTitle = pick.getTitle();
                    suggestedSlug = pick.getTitleSlug();
                    suggestedDifficulty = pick.getDifficulty();
                }

                result.add(new MemoryResponse.FadingConcept(
                        topic.getId().toString(),
                        topic.getTitle(),
                        topic.getCategory(),
                        topic.getConfidence(),
                        topic.getMastery(),
                        daysSinceRevision,
                        topic.getEstimatedRetention(),
                        suggestedTitle,
                        suggestedSlug,
                        suggestedDifficulty
                ));
            }
        }

        result.sort((a, b) -> {
            if (a.getConfidence() != b.getConfidence()) return Integer.compare(a.getConfidence(), b.getConfidence());
            if (a.getDaysSinceRevision() > b.getDaysSinceRevision()) return -1;
            if (a.getDaysSinceRevision() < b.getDaysSinceRevision()) return 1;
            return 0;
        });

        return result;
    }
}
