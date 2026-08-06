package com.forge.scheduler;

import com.forge.revision.entity.Revision;
import com.forge.revision.repository.RevisionRepository;
import com.forge.common.util.TimezoneUtil;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void materializeDueRevisions() {
        List<Topic> due = topicRepository.findTopicsNeedingRevision(LocalDateTime.now());
        int created = 0;
        int skipped = 0;
        for (Topic topic : due) {
            if (revisionRepository.existsByTopicIdAndCompletedFalse(topic.getId())) {
                skipped++;
                continue;
            }
            Revision revision = new Revision();
            revision.setUser(topic.getUser());
            revision.setTopic(topic);
            revision.setScheduledDate(LocalDate.now(TimezoneUtil.resolve(topic.getUser())));
            revision.setPriority(1);
            revision.setReason("scheduled");
            revision.setCompleted(false);
            revisionRepository.save(revision);
            created++;
        }
        if (created > 0) {
            log.info("RevisionScheduler: materialized {} revision(s) for due topics ({} already pending)", created, skipped);
        }
    }
}
