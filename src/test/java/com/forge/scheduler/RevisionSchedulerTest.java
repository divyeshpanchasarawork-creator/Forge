package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisionSchedulerTest {

    @Mock private TopicRepository topicRepository;
    @Mock private RevisionRepository revisionRepository;

    private RevisionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RevisionScheduler(topicRepository, revisionRepository);
    }

    private Topic dueTopic(String title) {
        Topic topic = new Topic();
        topic.setTitle(title);
        topic.setNextRevision(LocalDateTime.now().minusMinutes(5));
        User user = new User();
        user.setId(UUID.randomUUID());
        topic.setUser(user);
        return topic;
    }

    @Test
    void shouldCreateRevisionForDueTopicWithoutPendingRevision() {
        Topic topic = dueTopic("Binary Search");
        when(topicRepository.findTopicsNeedingRevision(any(LocalDateTime.class))).thenReturn(List.of(topic));
        when(revisionRepository.existsByTopicIdAndCompletedFalse(topic.getId())).thenReturn(false);

        scheduler.materializeDueRevisions();

        ArgumentCaptor<com.forge.revision.entity.Revision> captor = ArgumentCaptor.forClass(com.forge.revision.entity.Revision.class);
        verify(revisionRepository, times(1)).save(captor.capture());
        assertEquals(topic, captor.getValue().getTopic());
        assertEquals(topic.getUser(), captor.getValue().getUser());
        assertEquals("scheduled", captor.getValue().getReason());
        assertEquals(false, captor.getValue().getCompleted());
    }

    @Test
    void shouldSkipTopicWithExistingPendingRevision() {
        Topic topic = dueTopic("Arrays");
        when(topicRepository.findTopicsNeedingRevision(any(LocalDateTime.class))).thenReturn(List.of(topic));
        when(revisionRepository.existsByTopicIdAndCompletedFalse(topic.getId())).thenReturn(true);

        scheduler.materializeDueRevisions();

        verify(revisionRepository, never()).save(any());
    }

    @Test
    void shouldDoNothingWhenNoTopicsAreDue() {
        when(topicRepository.findTopicsNeedingRevision(any(LocalDateTime.class))).thenReturn(List.of());

        scheduler.materializeDueRevisions();

        verify(revisionRepository, never()).save(any());
    }

    @Test
    void shouldCreateRevisionsForMultipleDueTopics() {
        Topic t1 = dueTopic("Binary Search");
        Topic t2 = dueTopic("Two Pointers");
        when(topicRepository.findTopicsNeedingRevision(any(LocalDateTime.class))).thenReturn(List.of(t1, t2));
        when(revisionRepository.existsByTopicIdAndCompletedFalse(t1.getId())).thenReturn(false);
        when(revisionRepository.existsByTopicIdAndCompletedFalse(t2.getId())).thenReturn(false);

        scheduler.materializeDueRevisions();

        verify(revisionRepository, times(2)).save(any());
        assertTrue(t1.getNextRevision() != null && t2.getNextRevision() != null);
    }
}
