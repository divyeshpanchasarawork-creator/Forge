package com.forge.revision.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.revision.entity.Revision;
import com.forge.revision.mapper.RevisionMapper;
import com.forge.revision.repository.RevisionRepository;
import com.forge.security.UserPrincipal;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

    @Mock private RevisionRepository revisionRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpacedRepetitionService spacedRepetitionService;

    private RevisionService service;
    private UUID userId;
    private UUID topicId;
    private UUID revisionId;

    @BeforeEach
    void setUp() {
        service = new RevisionService(revisionRepository, topicRepository, userRepository,
                new RevisionMapper(), spacedRepetitionService);
        userId = UUID.randomUUID();
        topicId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password", "USER");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User currentUser() {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Revision revision(User user, Topic topic) {
        Revision revision = new Revision();
        revision.setId(revisionId);
        revision.setUser(user);
        revision.setTopic(topic);
        revision.setCompleted(false);
        return revision;
    }

    @Test
    void todayRevisionsUseTheUsersTimezone() {
        ZoneId zone = ZoneId.of("America/New_York");
        User user = currentUser();
        user.setTimezone(zone.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(revisionRepository.findByUserIdAndScheduledDateAndCompleted(eq(userId), eq(LocalDate.now(zone)), eq(false)))
                .thenReturn(List.of());

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            service.getTodayRevisions();
        }

        verify(revisionRepository).findByUserIdAndScheduledDateAndCompleted(userId, LocalDate.now(zone), false);
    }

    @Test
    void todayActivityIncludesCompletedAndPendingForUsersTimezone() {
        ZoneId zone = ZoneId.of("America/New_York");
        User user = currentUser();
        user.setTimezone(zone.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(revisionRepository.findActivityByUserIdAndScheduledDate(eq(userId), eq(LocalDate.now(zone))))
                .thenReturn(List.of());

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            service.getTodayActivity();
        }

        verify(revisionRepository).findActivityByUserIdAndScheduledDate(userId, LocalDate.now(zone));
    }

    @Test
    void pendingRevisionsUseTheUsersTimezone() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        User user = currentUser();
        user.setTimezone(zone.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(revisionRepository.findPendingRevisionsByUserId(eq(userId), eq(LocalDate.now(zone)))).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            service.getPendingRevisions();
        }

        verify(revisionRepository).findPendingRevisionsByUserId(userId, LocalDate.now(zone));
    }

    @Test
    void completeRevisionAppliesSm2FieldsToTopic() {
        User user = currentUser();
        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setUser(user);
        topic.setTitle("Binary Search");
        topic.setCategory("ARRAY");
        topic.setEasinessFactor(2.5);
        topic.setRepetitionInterval(6);
        topic.setRevisionCount(2);
        topic.setMastery(50);
        topic.setStatus("IN_PROGRESS");
        Revision revision = revision(user, topic);

        when(revisionRepository.findByIdWithTopic(revisionId)).thenReturn(Optional.of(revision));
        when(spacedRepetitionService.calculate(topic, 4))
                .thenReturn(new SpacedRepetitionService.Sm2Result(10, 2.6, 8));
        when(revisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            service.completeRevision(revisionId, 4);
        }

        assertTrue(revision.getCompleted());
        assertNotNull(revision.getCompletionDate());
        assertEquals(2.6, topic.getEasinessFactor());
        assertEquals(10, topic.getRepetitionInterval());
        assertEquals(3, topic.getRevisionCount());
        assertEquals(58, topic.getMastery());
        assertNotNull(topic.getNextRevision());
        assertEquals("IN_PROGRESS", topic.getStatus());
        verify(topicRepository).save(topic);
    }

    @Test
    void lowQualityRevisionResetsRevisionCountAndDoesNotBoostMastery() {
        User user = currentUser();
        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setUser(user);
        topic.setRevisionCount(3);
        topic.setMastery(40);
        topic.setStatus("IN_PROGRESS");
        Revision revision = revision(user, topic);

        when(revisionRepository.findByIdWithTopic(revisionId)).thenReturn(Optional.of(revision));
        when(spacedRepetitionService.calculate(topic, 2))
                .thenReturn(new SpacedRepetitionService.Sm2Result(1, 2.4, 0));
        when(revisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            service.completeRevision(revisionId, 2);
        }

        assertEquals(0, topic.getRevisionCount());
        assertEquals(40, topic.getMastery());
    }

    @Test
    void masteredTopicIsMarkedMastered() {
        User user = currentUser();
        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setUser(user);
        topic.setMastery(75);
        topic.setStatus("IN_PROGRESS");
        Revision revision = revision(user, topic);

        when(revisionRepository.findByIdWithTopic(revisionId)).thenReturn(Optional.of(revision));
        when(spacedRepetitionService.calculate(topic, 5))
                .thenReturn(new SpacedRepetitionService.Sm2Result(12, 2.7, 10));
        when(revisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            service.completeRevision(revisionId, 5);
        }

        assertEquals(85, topic.getMastery());
        assertEquals("MASTERED", topic.getStatus());
    }

    @Test
    void completingTwiceIsRejected() {
        User user = currentUser();
        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setUser(user);
        Revision revision = revision(user, topic);
        revision.setCompleted(true);

        when(revisionRepository.findByIdWithTopic(revisionId)).thenReturn(Optional.of(revision));

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            assertThrows(BadRequestException.class, () -> service.completeRevision(revisionId, 4));
        }
        verify(topicRepository, never()).save(any());
    }

    @Test
    void completingSomeoneElsesRevisionIsRejected() {
        User other = new User();
        other.setId(UUID.randomUUID());
        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setUser(other);
        Revision revision = revision(other, topic);

        when(revisionRepository.findByIdWithTopic(revisionId)).thenReturn(Optional.of(revision));

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            assertThrows(ResourceNotFoundException.class, () -> service.completeRevision(revisionId, 4));
        }
        verify(topicRepository, never()).save(any());
    }
}
