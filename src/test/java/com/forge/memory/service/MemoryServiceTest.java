package com.forge.memory.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.SecurityUtils;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.memory.dto.MemoryResponse;
import com.forge.security.UserPrincipal;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock private TopicRepository topicRepository;
    @Mock private LeetCodeTagStatRepository tagStatRepository;
    @Mock private ProblemLoader problemLoader;
    @Mock private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();

    private MemoryService service() {
        return new MemoryService(topicRepository, tagStatRepository, problemLoader, userRepository);
    }

    @Test
    void neverAttemptedColdStartTopicsAreNotFading() {
        User user = new User();
        user.setId(userId);
        user.setTimezone("UTC");

        Topic coldStart = new Topic();
        coldStart.setId(UUID.randomUUID());
        coldStart.setSource("COLD_START");
        coldStart.setAttemptsTotal(0);
        coldStart.setTitle("Graphs");
        coldStart.setCategory("ALGORITHM");
        coldStart.setConfidence(0);

        Topic engaged = new Topic();
        engaged.setId(UUID.randomUUID());
        engaged.setTitle("Binary Search");
        engaged.setCategory("ARRAY");
        engaged.setConfidence(2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findByUserId(userId, PageRequest.of(0, 1000)))
                .thenReturn(List.of(coldStart, engaged));
        when(problemLoader.getProblemsForTag(any())).thenReturn(List.of());

        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password", "USER");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MemoryResponse response;
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            response = service().getMemory();
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertEquals(1, response.getFadingConcepts().size());
        assertEquals("Binary Search", response.getFadingConcepts().get(0).getTitle());
    }
}
