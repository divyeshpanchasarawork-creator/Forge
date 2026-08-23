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
import com.forge.leetcode.dto.PendingSolveResponse;
import com.forge.leetcode.entity.ExternalSolve;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.ExternalSolveRepository;
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
    @Mock private ExternalSolveRepository externalSolveRepository;
    @Mock private UserRepository userRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LeetCodeTopicMapper topicMapper;
    @Mock private RecommendationEngine recommendationEngine;
    @Mock private ProblemLoader problemLoader;
    @Mock private PlatformTransactionManager transactionManager;

    private LeetCodeFetchService buildService() {
        return new LeetCodeFetchService(leetCodeClient, snapshotRepository, tagStatRepository,
                problemSuggestionRepository, externalSolveRepository, userRepository, topicRepository, topicMapper,
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
    void syncUpsertsNewExternalSolvesAndSkipsKnownOnes() {
        User user = userWithLeetcode("forgeleet");
        LeetCodeGraphQlResponse response = responseWithMatchedUser();
        LeetCodeGraphQlResponse.RecentSubmission fresh = new LeetCodeGraphQlResponse.RecentSubmission();
        fresh.setTitle("Two Sum");
        fresh.setTitleSlug("two-sum");
        fresh.setTimestamp("1700000000");
        LeetCodeGraphQlResponse.RecentSubmission known = new LeetCodeGraphQlResponse.RecentSubmission();
        known.setTitle("Add Two Numbers");
        known.setTitleSlug("add-two-numbers");
        known.setTimestamp("1700001000");
        response.getData().setRecentAcSubmissionList(List.of(fresh, known));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(leetCodeClient.fetchUserProfile("forgeleet")).thenReturn(response);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(snapshotRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tagStatRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(topicMapper.mapToTopics(any(), any(), eq("mixed"))).thenReturn(List.of());
        when(externalSolveRepository.existsByUserIdAndTitleSlug(user.getId(), "two-sum")).thenReturn(false);
        when(externalSolveRepository.existsByUserIdAndTitleSlug(user.getId(), "add-two-numbers")).thenReturn(true);
        when(externalSolveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        buildService().syncUserProfile(user.getId());

        org.mockito.ArgumentCaptor<ExternalSolve> captor = org.mockito.ArgumentCaptor.forClass(ExternalSolve.class);
        verify(externalSolveRepository, times(1)).save(captor.capture());
        ExternalSolve saved = captor.getValue();
        assertEquals("two-sum", saved.getTitleSlug());
        assertEquals("Two Sum", saved.getTitle());
        assertFalse(saved.isLogged());
        assertNotNull(saved.getSolvedAt());
    }

    @Test
    void syncSurvivesExternalSolveDetectionFailure() {
        User user = userWithLeetcode("forgeleet");
        LeetCodeGraphQlResponse response = responseWithMatchedUser();
        LeetCodeGraphQlResponse.RecentSubmission submission = new LeetCodeGraphQlResponse.RecentSubmission();
        submission.setTitle("Two Sum");
        submission.setTitleSlug("two-sum");
        submission.setTimestamp("1700000000");
        response.getData().setRecentAcSubmissionList(List.of(submission));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(leetCodeClient.fetchUserProfile("forgeleet")).thenReturn(response);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(snapshotRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tagStatRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(topicMapper.mapToTopics(any(), any(), eq("mixed"))).thenReturn(List.of());
        when(externalSolveRepository.existsByUserIdAndTitleSlug(any(), any()))
                .thenThrow(new RuntimeException("db blew up"));

        LeetCodeStatsResponse stats = buildService().syncUserProfile(user.getId());

        assertEquals(42, stats.getTotalSolved());
        verify(recommendationEngine).generateForUser(user.getId(), true);
    }

    @Test
    void getPendingSolvesOnlySurfacesLibraryMatches() {
        UUID userId = UUID.randomUUID();
        ExternalSolve matched = new ExternalSolve();
        matched.setId(UUID.randomUUID());
        matched.setTitleSlug("two-sum");
        matched.setTitle("Two Sum");
        ExternalSolve outside = new ExternalSolve();
        outside.setId(UUID.randomUUID());
        outside.setTitleSlug("not-in-library");

        when(externalSolveRepository.findByUserIdAndLoggedFalseOrderBySolvedAtDesc(userId))
                .thenReturn(List.of(matched, outside));
        when(problemLoader.getTagSlugForProblem("two-sum")).thenReturn("arrays");
        when(problemLoader.getTagSlugForProblem("not-in-library")).thenReturn(null);
        when(problemLoader.getProblemsForTag("arrays"))
                .thenReturn(List.of(new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy")));

        List<PendingSolveResponse> pending = buildService().getPendingSolves(userId);

        assertEquals(1, pending.size());
        PendingSolveResponse dto = pending.get(0);
        assertEquals("two-sum", dto.titleSlug());
        assertEquals("arrays", dto.topicTagSlug());
        assertEquals("Easy", dto.difficulty());
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
