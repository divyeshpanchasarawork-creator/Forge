package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.TimezoneUtil;
import com.forge.revision.entity.Revision;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Materializes {@link Revision} rows for topics whose {@code nextRevision} has arrived.
 * Spaced-repetition scheduling keeps the next-due date on the topic; this job turns a due
 * topic into an actionable revision item (dashboard / analytics / overdue recommendations all
 * read the {@code revisions} table). Rows are de-duplicated against existing pending revisions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevisionScheduler {

    private final TopicRepository topicRepository;
    private final RevisionRepository revisionRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 */30 * * * *")
    public void materializeDueRevisions() {
        // Due dates are day-granular and stored in each user's timezone, so the "today" cutoff
        // must be resolved per user — never the server clock.
        // Single bulk query instead of one exists-check per due topic (N+1).
        Set<UUID> pendingTopicIds = new HashSet<>(revisionRepository.findTopicIdsWithPendingRevision());

        for (User user : userRepository.findAll()) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    LocalDate today = TimezoneUtil.today(user);
                    List<Topic> due = topicRepository.findTopicsNeedingRevisionByUserId(user.getId(), today);
                    List<Revision> toCreate = due.stream()
                            .filter(topic -> !pendingTopicIds.contains(topic.getId()))
                            .map(topic -> buildRevision(user, topic, today))
                            .toList();
                    if (!toCreate.isEmpty()) {
                        try {
                            revisionRepository.saveAll(toCreate);
                            log.info("RevisionScheduler: materialized {} revision(s) for user {} ({} already pending)",
                                    toCreate.size(), user.getId(), due.size() - toCreate.size());
                        } catch (DataIntegrityViolationException e) {
                            // A concurrent scheduler instance already materialized these rows; the
                            // partial unique index (uq_revisions_pending) is the real guard.
                            log.debug("RevisionScheduler: {} revision(s) for user {} already materialized (concurrent run); skipping",
                                    toCreate.size(), user.getId());
                        }
                    }
                });
            } catch (RuntimeException e) {
                log.error("RevisionScheduler: failed to materialize revisions for user {}", user.getId(), e);
            }
        }
    }

    private Revision buildRevision(User user, Topic topic, LocalDate today) {
        Revision revision = new Revision();
        revision.setUser(user);
        revision.setTopic(topic);
        revision.setScheduledDate(today);
        revision.setPriority(1);
        revision.setReason("scheduled");
        revision.setCompleted(false);
        revision.setPendingTopic(topic.getId());
        return revision;
    }
}
