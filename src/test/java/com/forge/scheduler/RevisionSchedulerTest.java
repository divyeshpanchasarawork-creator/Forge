package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.revision.entity.Revision;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisionSchedulerTest {

    @Mock private TopicRepository topicRepository;
    @Mock private RevisionRepository revisionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private RevisionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RevisionScheduler(topicRepository, revisionRepository, userRepository,
                new TransactionTemplate(transactionManager));
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Topic dueTopic(String title, User user) {
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle(title);
        topic.setNextRevision(LocalDate.now().minusDays(1));
        topic.setUser(user);
        return topic;
    }

    @Test
    void shouldCreateRevisionForDueTopicWithoutPendingRevision() {
        User user = user();
        Topic topic = dueTopic("Binary Search", user);
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(topicRepository.findTopicsNeedingRevisionByUserId(eq(user.getId()), any())).thenReturn(List.of(topic));
        when(revisionRepository.findTopicIdsWithPendingRevision()).thenReturn(List.of());

        scheduler.materializeDueRevisions();

        ArgumentCaptor<LocalDate> todayCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(topicRepository).findTopicsNeedingRevisionByUserId(eq(user.getId()), todayCaptor.capture());
        assertEquals(LocalDate.now(ZoneId.of("UTC")), todayCaptor.getValue());

        ArgumentCaptor<List<Revision>> captor = ArgumentCaptor.forClass(List.class);
        verify(revisionRepository).saveAll(captor.capture());
        Revision saved = captor.getValue().getFirst();
        assertEquals(topic, saved.getTopic());
        assertEquals(user, saved.getUser());
        assertEquals("scheduled", saved.getReason());
        assertEquals(false, saved.getCompleted());
        assertEquals(LocalDate.now(ZoneId.of("UTC")), saved.getScheduledDate());
    }

    @Test
    void shouldSkipTopicWithExistingPendingRevision() {
        User user = user();
        Topic topic = dueTopic("Arrays", user);
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(topicRepository.findTopicsNeedingRevisionByUserId(eq(user.getId()), any())).thenReturn(List.of(topic));
        when(revisionRepository.findTopicIdsWithPendingRevision()).thenReturn(List.of(topic.getId()));

        scheduler.materializeDueRevisions();

        verify(revisionRepository, never()).saveAll(any());
    }

    @Test
    void shouldDoNothingWhenNoTopicsAreDue() {
        User user = user();
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(topicRepository.findTopicsNeedingRevisionByUserId(eq(user.getId()), any())).thenReturn(List.of());

        scheduler.materializeDueRevisions();

        verify(revisionRepository, never()).saveAll(any());
    }

    @Test
    void shouldCreateRevisionsForMultipleDueTopics() {
        User user = user();
        Topic t1 = dueTopic("Binary Search", user);
        Topic t2 = dueTopic("Two Pointers", user);
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(topicRepository.findTopicsNeedingRevisionByUserId(eq(user.getId()), any())).thenReturn(List.of(t1, t2));
        when(revisionRepository.findTopicIdsWithPendingRevision()).thenReturn(List.of());

        scheduler.materializeDueRevisions();

        ArgumentCaptor<List<Revision>> captor = ArgumentCaptor.forClass(List.class);
        verify(revisionRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(t1.getNextRevision() != null && t2.getNextRevision() != null);
    }
}
