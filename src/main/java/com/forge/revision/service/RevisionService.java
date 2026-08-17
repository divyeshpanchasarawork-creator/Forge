package com.forge.revision.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TimezoneUtil;
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

    @Transactional(readOnly = true)
    public List<RevisionResponse> getTodayRevisions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        java.time.ZoneId zone = TimezoneUtil.resolve(userRepository.findById(userId).orElse(null));
        List<Revision> revisions = revisionRepository.findByUserIdAndScheduledDateAndCompleted(userId, LocalDate.now(zone), false);
        return revisions.stream().map(revisionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RevisionResponse> getTodayActivity() {
        UUID userId = SecurityUtils.getCurrentUserId();
        java.time.ZoneId zone = TimezoneUtil.resolve(userRepository.findById(userId).orElse(null));
        List<Revision> revisions = revisionRepository.findActivityByUserIdAndScheduledDate(userId, LocalDate.now(zone));
        return revisions.stream().map(revisionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RevisionResponse> getPendingRevisions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        java.time.ZoneId zone = TimezoneUtil.resolve(userRepository.findById(userId).orElse(null));
        List<Revision> revisions = revisionRepository.findPendingRevisionsByUserId(userId, LocalDate.now(zone));
        return revisions.stream().map(revisionMapper::toResponse).toList();
    }

    @Transactional
    public RevisionResponse completeRevision(UUID id, int quality) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Revision revision = revisionRepository.findByIdWithTopic(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revision", "id", id));

        if (!revision.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Revision", "id", id);
        }

        if (revision.getCompleted()) {
            throw new BadRequestException("Revision already completed");
        }

        LocalDateTime now = TimezoneUtil.now(revision.getUser());

        revision.setCompleted(true);
        revision.setCompletionDate(now);
        revision.setPendingTopic(null);
        revisionRepository.save(revision);

        Topic topic = revision.getTopic();
        topic.setLastRevision(now);

        SpacedRepetitionService.Sm2Result sm2 = spacedRepetitionService.calculate(topic, quality);
        topic.setEasinessFactor(sm2.easinessFactor());
        topic.setRepetitionInterval(sm2.intervalDays());
        topic.setLastQuality(quality);
        topic.setNextRevision(now.toLocalDate().plusDays(sm2.intervalDays()));

        if (quality < 3) {
            topic.setRevisionCount(0);
        } else {
            topic.setRevisionCount((topic.getRevisionCount() != null ? topic.getRevisionCount() : 0) + 1);
        }

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
}
