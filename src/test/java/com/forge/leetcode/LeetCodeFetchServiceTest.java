package com.forge.leetcode;

import com.forge.common.util.ProblemLoader;
import com.forge.leetcode.client.LeetCodeClient;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.ExternalSolveRepository;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.leetcode.service.LeetCodeFetchService;
import com.forge.leetcode.service.LeetCodeTopicMapper;
import com.forge.auth.repository.UserRepository;
import com.forge.recommendation.service.RecommendationEngine;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeetCodeFetchServiceTest {

    @Mock private LeetCodeClient leetCodeClient;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;
    @Mock private LeetCodeTagStatRepository tagStatRepository;
    @Mock private ProblemSuggestionRepository problemSuggestionRepository;
    @Mock private ExternalSolveRepository externalSolveRepository;
    @Mock private UserRepository userRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LeetCodeTopicMapper topicMapper;
    @Mock private RecommendationEngine recommendationEngine;
    @Mock private ProblemLoader problemLoader;
    @Mock private PlatformTransactionManager transactionManager;

    private LeetCodeFetchService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new LeetCodeFetchService(leetCodeClient, snapshotRepository, tagStatRepository,
                problemSuggestionRepository, externalSolveRepository, userRepository, topicRepository, topicMapper,
                recommendationEngine, problemLoader, transactionManager);
        userId = UUID.randomUUID();
    }

    private LeetCodeTagStat weakTag(String slug, String name) {
        LeetCodeTagStat stat = new LeetCodeTagStat();
        stat.setTagSlug(slug);
        stat.setTagName(name);
        stat.setProblemsSolved(2);
        return stat;
    }

    @Test
    void refreshShouldDeleteWeakTagSuggestionsEvenWhenNoneGenerated() {
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of(weakTag("arrays", "Array")));
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(List.of());

        service.refreshProblemSuggestions(userId);

        verify(problemSuggestionRepository).deleteByUserIdAndSource(userId, "WEAK_TAG");
        verify(problemSuggestionRepository, never()).saveAll(any());
    }

    @Test
    void refreshShouldReplaceWeakTagSuggestionsWhenGenerated() {
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of(weakTag("arrays", "Array")));
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(
                List.of(new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy")));

        service.refreshProblemSuggestions(userId);

        verify(problemSuggestionRepository).deleteByUserIdAndSource(userId, "WEAK_TAG");
        verify(problemSuggestionRepository).saveAll(any());
    }
}
