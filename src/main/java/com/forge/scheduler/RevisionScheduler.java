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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void materializeDueRevisions() {
        // Due dates are day-granular and stored in each user's timezone, so the "today" cutoff
        // must be resolved per user — never the server clock.
        // Single bulk query instead of one exists-check per due topic (N+1).
        Set<UUID> pendingTopicIds = new HashSet<>(revisionRepository.findTopicIdsWithPendingRevision());

        int created = 0;
        int skipped = 0;
        for (User user : userRepository.findAll()) {
            List<Topic> due = topicRepository.findTopicsNeedingRevisionByUserId(user.getId(), TimezoneUtil.today(user));
            for (Topic topic : due) {
                if (pendingTopicIds.contains(topic.getId())) {
                    skipped++;
                    continue;
                }
                Revision revision = new Revision();
                revision.setUser(user);
                revision.setTopic(topic);
                revision.setScheduledDate(TimezoneUtil.today(user));
                revision.setPriority(1);
                revision.setReason("scheduled");
                revision.setCompleted(false);
                revisionRepository.save(revision);
                created++;
            }
        }
        if (created > 0) {
            log.info("RevisionScheduler: materialized {} revision(s) for due topics ({} already pending)", created, skipped);
        }
    }
}
