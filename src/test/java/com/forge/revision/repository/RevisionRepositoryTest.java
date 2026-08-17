package com.forge.revision.repository;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.revision.entity.Revision;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:revision-repo-test;DB_CLOSE_DELAY=-1")
class RevisionRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private RevisionRepository revisionRepository;

    private User newUser() {
        User user = new User();
        user.setUsername("rev-" + UUID.randomUUID().toString().substring(0, 8));
        return userRepository.saveAndFlush(user);
    }

    private Topic newTopic(User user, LocalDate nextRevision) {
        Topic topic = new Topic();
        topic.setUser(user);
        topic.setTitle("Two Pointers");
        topic.setCategory("ARRAYS");
        topic.setNextRevision(nextRevision);
        return topicRepository.saveAndFlush(topic);
    }

    private Revision newRevision(User user, Topic topic, LocalDate scheduledDate, boolean completed) {
        Revision revision = new Revision();
        revision.setUser(user);
        revision.setTopic(topic);
        revision.setScheduledDate(scheduledDate);
        revision.setCompleted(completed);
        revision.setPendingTopic(completed ? null : topic.getId());
        return revisionRepository.saveAndFlush(revision);
    }

    @Test
    void findPendingRevisionsByUserIdReturnsOnlyOverduePendingWithTopicFetched() {
        User user = newUser();
        Topic overdue = newTopic(user, LocalDate.now().minusDays(2));
        Topic future = newTopic(user, LocalDate.now().plusDays(5));
        newRevision(user, overdue, LocalDate.now().minusDays(2), false);
        newRevision(user, future, LocalDate.now().plusDays(5), false);
        newRevision(user, overdue, LocalDate.now().minusDays(1), true);

        List<Revision> result = revisionRepository.findPendingRevisionsByUserId(user.getId(), LocalDate.now());

        assertEquals(1, result.size());
        Revision revision = result.get(0);
        assertEquals("Two Pointers", revision.getTopic().getTitle());
        assertFalse(revision.getCompleted());
    }

    @Test
    void findActivityByUserIdAndScheduledDateOrdersCompletedLast() {
        User user = newUser();
        Topic topic = newTopic(user, LocalDate.now());
        newRevision(user, topic, LocalDate.now(), true);
        newRevision(user, topic, LocalDate.now(), false);

        List<Revision> result = revisionRepository.findActivityByUserIdAndScheduledDate(user.getId(), LocalDate.now());

        assertEquals(2, result.size());
        assertFalse(result.get(0).getCompleted());
        assertTrue(result.get(1).getCompleted());
    }

    @Test
    void findTopicsNeedingRevisionUsesDayGranularCutoffPerUser() {
        User user = newUser();
        User other = newUser();
        Topic dueToday = newTopic(user, LocalDate.now());
        Topic dueTomorrow = newTopic(user, LocalDate.now().plusDays(1));
        Topic otherUser = newTopic(other, LocalDate.now().minusDays(10));

        List<Topic> result = topicRepository.findTopicsNeedingRevisionByUserId(user.getId(), LocalDate.now());

        assertEquals(List.of(dueToday.getId()), result.stream().map(Topic::getId).toList());
        assertFalse(result.contains(otherUser));
    }

    @Test
    void existsByTopicIdAndCompletedFalseRespectsCompletion() {
        User user = newUser();
        Topic open = newTopic(user, LocalDate.now());
        Topic done = newTopic(user, LocalDate.now().plusDays(1));
        newRevision(user, open, LocalDate.now(), false);
        newRevision(user, done, LocalDate.now(), true);

        assertTrue(revisionRepository.existsByTopicIdAndCompletedFalse(open.getId()));
        assertFalse(revisionRepository.existsByTopicIdAndCompletedFalse(done.getId()));
    }
}
