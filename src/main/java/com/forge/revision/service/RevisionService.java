package com.forge.revision.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.revision.dto.RevisionResponse;
import com.forge.revision.entity.Revision;
import com.forge.revision.mapper.RevisionMapper;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevisionService {

    private final RevisionRepository revisionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final RevisionMapper revisionMapper;
    private final SpacedRepetitionService spacedRepetitionService;

    public List<RevisionResponse> getTodayRevisions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Revision> revisions = revisionRepository.findByUserIdAndScheduledDateAndCompleted(userId, LocalDate.now(), false);
        return revisions.stream().map(revisionMapper::toResponse).toList();
    }

    public List<RevisionResponse> getPendingRevisions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Revision> revisions = revisionRepository.findPendingRevisionsByUserId(userId, LocalDate.now());
        return revisions.stream().map(revisionMapper::toResponse).toList();
    }

    @Transactional
    public RevisionResponse completeRevision(UUID id, int quality) {
        Revision revision = revisionRepository.findByIdWithTopic(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revision", "id", id));

        if (revision.getCompleted()) {
            throw new BadRequestException("Revision already completed");
        }

        revision.setCompleted(true);
        revision.setCompletionDate(LocalDateTime.now());
        revisionRepository.save(revision);

        Topic topic = revision.getTopic();
        topic.setRevisionCount(topic.getRevisionCount() + 1);
        topic.setLastRevision(LocalDateTime.now());

        SpacedRepetitionService.Sm2Result sm2 = spacedRepetitionService.calculate(topic, quality);
        topic.setEasinessFactor(sm2.easinessFactor());
        topic.setRepetitionInterval(sm2.intervalDays());
        topic.setLastQuality(quality);
        topic.setNextRevision(LocalDateTime.now().plusDays(sm2.intervalDays()));

        int masteryBoost = sm2.masteryBoost();
        topic.setMastery(Math.max(0, Math.min(100, (topic.getMastery() != null ? topic.getMastery() : 0) + masteryBoost)));

        if (topic.getMastery() >= 80) {
            topic.setStatus("MASTERED");
        } else if (topic.getMastery() > 0 || topic.getRevisionCount() > 0) {
            topic.setStatus("IN_PROGRESS");
        }

        topicRepository.save(topic);
        log.info("Revision completed for topic: {}, mastery now: {}, next interval: {}d, EF: {}",
                topic.getTitle(), topic.getMastery(), sm2.intervalDays(), sm2.easinessFactor());

        return revisionMapper.toResponse(revision);
    }

    public long getCompletedCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return revisionRepository.countByUserIdAndCompleted(userId, true);
    }

    public long getCompletedInRange(LocalDate start, LocalDate end) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return revisionRepository.countCompletedInRangeByUserId(userId, start, end);
    }
}
