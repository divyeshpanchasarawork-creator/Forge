package com.forge.leetcode.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.exception.ServiceUnavailableException;
import com.forge.common.util.ProblemLoader;
import com.forge.leetcode.client.LeetCodeClient;
import com.forge.leetcode.dto.LeetCodeGraphQlResponse;
import com.forge.leetcode.dto.LeetCodeStatsResponse;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.recommendation.service.RecommendationEngine;
import com.forge.leetcode.service.LeetCodeTopicMapper;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeetCodeFetchServiceTest {

    @Mock private LeetCodeClient leetCodeClient;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;
    @Mock private LeetCodeTagStatRepository tagStatRepository;
    @Mock private ProblemSuggestionRepository problemSuggestionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LeetCodeTopicMapper topicMapper;
    @Mock private RecommendationEngine recommendationEngine;
    @Mock private ProblemLoader problemLoader;
    @Mock private PlatformTransactionManager transactionManager;

    private LeetCodeFetchService buildService() {
        return new LeetCodeFetchService(leetCodeClient, snapshotRepository, tagStatRepository,
                problemSuggestionRepository, userRepository, topicRepository, topicMapper,
                recommendationEngine, problemLoader, transactionManager);
    }

    private User userWithLeetcode(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("forge");
        user.setLeetcodeUsername(username);
        return user;
    }

    private LeetCodeGraphQlResponse responseWithMatchedUser() {
        LeetCodeGraphQlResponse.MatchedUser matchedUser = new LeetCodeGraphQlResponse.MatchedUser();
        matchedUser.setTagProblemCounts(new LeetCodeGraphQlResponse.TagProblemCountGroup());

        LeetCodeGraphQlResponse.DifficultyCount all = new LeetCodeGraphQlResponse.DifficultyCount();
        all.setDifficulty("All");
        all.setCount(42);
        LeetCodeGraphQlResponse.DifficultyCount easy = new LeetCodeGraphQlResponse.DifficultyCount();
        easy.setDifficulty("Easy");
        easy.setCount(20);
        LeetCodeGraphQlResponse.SubmitStatsGlobal stats = new LeetCodeGraphQlResponse.SubmitStatsGlobal();
        stats.setAcSubmissionNum(List.of(all, easy));
        matchedUser.setSubmitStatsGlobal(stats);

        LeetCodeGraphQlResponse.Data data = new LeetCodeGraphQlResponse.Data();
        data.setMatchedUser(matchedUser);
        LeetCodeGraphQlResponse response = new LeetCodeGraphQlResponse();
        response.setData(data);
        return response;
    }

    @Test
    void syncUserProfileWritesDataAndGeneratesRecommendations() {
        User user = userWithLeetcode("forgeleet");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(leetCodeClient.fetchUserProfile("forgeleet")).thenReturn(responseWithMatchedUser());
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(snapshotRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tagStatRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(topicMapper.mapToTopics(any(), any(), eq("mixed"))).thenReturn(List.of());

        LeetCodeStatsResponse response = buildService().syncUserProfile(user.getId());

        assertEquals(42, response.getTotalSolved());
        assertEquals(20, response.getEasySolved());
        verify(snapshotRepository).save(any());
        verify(tagStatRepository).deleteByUserId(user.getId());
        verify(topicRepository).saveAll(any());
        verify(problemSuggestionRepository).deleteByUserIdAndSource(user.getId(), "WEAK_TAG");
        verify(recommendationEngine).generateForUser(user.getId(), true);
    }

    @Test
    void syncUserProfileRejectsMissingLeetcodeUsername() {
        User user = userWithLeetcode(null);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> buildService().syncUserProfile(user.getId()));
        verify(leetCodeClient, never()).fetchUserProfile(any());
    }

    @Test
    void syncUserProfileRejectsNullUpstreamResponse() {
        User user = userWithLeetcode("forgeleet");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(leetCodeClient.fetchUserProfile("forgeleet")).thenReturn(null);

        assertThrows(ServiceUnavailableException.class, () -> buildService().syncUserProfile(user.getId()));
    }

    @Test
    void syncUserProfileRejectsMissingMatchedUser() {
        User user = userWithLeetcode("forgeleet");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        LeetCodeGraphQlResponse response = new LeetCodeGraphQlResponse();
        response.setData(new LeetCodeGraphQlResponse.Data());
        when(leetCodeClient.fetchUserProfile("forgeleet")).thenReturn(response);

        assertThrows(ResourceNotFoundException.class, () -> buildService().syncUserProfile(user.getId()));
    }

    @Test
    void syncUserProfileRejectsMissingTagProblemCounts() {
        User user = userWithLeetcode("forgeleet");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        LeetCodeGraphQlResponse.MatchedUser matchedUser = new LeetCodeGraphQlResponse.MatchedUser();
        LeetCodeGraphQlResponse.Data data = new LeetCodeGraphQlResponse.Data();
        data.setMatchedUser(matchedUser);
        LeetCodeGraphQlResponse response = new LeetCodeGraphQlResponse();
        response.setData(data);
        when(leetCodeClient.fetchUserProfile("forgeleet")).thenReturn(response);

        assertThrows(ServiceUnavailableException.class, () -> buildService().syncUserProfile(user.getId()));
    }

    @Test
    void getLatestStatsReturnsNullWhenNoSnapshot() {
        when(snapshotRepository.findByUserId(any())).thenReturn(Optional.empty());

        assertNull(buildService().getLatestStats(UUID.randomUUID()));
    }
}
