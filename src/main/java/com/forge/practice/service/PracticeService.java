package com.forge.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.exception.BadRequestException;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TimezoneUtil;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.MasteryService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.knowledge.service.KnowledgeGraphService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.dto.PracticeQueueResponse;
import com.forge.practice.dto.ProblemAttemptDto;
import com.forge.practice.dto.ProblemAttemptRequest;
import com.forge.practice.dto.ProblemAttemptResponse;
import com.forge.practice.dto.ProblemAttemptSummary;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.recommendation.service.CandidatePoolService;
import com.forge.recommendation.service.RecommendationService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeService {

    private static final Set<String> VALID_OUTCOMES = Set.of("SOLVED", "FAILED", "PARTIAL", "SKIPPED");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProblemScorer problemScorer;
    private final ProblemAttemptRepository problemAttemptRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ColdStartService coldStartService;
    private final SessionPlanner sessionPlanner;
    private final MasteryService masteryService;
    private final SkillRatingService skillRatingService;
    private final ForgettingCurveService forgettingCurveService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final RecommendationService recommendationService;
    private final CandidatePoolService candidatePoolService;

    public PracticeQueueResponse getPracticeQueue() {
        UUID userId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = TimezoneUtil.now(userRepository.findById(userId).orElse(null));

        if (coldStartService.needsSeed(userId)) {
            coldStartService.seedStarterTopics(userId);
        }
        ColdStartService.Profile profile = coldStartService.classify(userId);

        ProblemScorer.ScoringContext ctx = problemScorer.context(userId);
        List<CandidatePoolService.Candidate> scored = candidatePoolService.rankForUser(ctx, CandidatePoolService.MAX_CANDIDATES);
        Map<String, SessionPlanner.AttemptCounts> attemptsBySlug = attemptsBySlug(ctx);

        List<Topic> revisionTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 500)).stream()
                .filter(t -> (t.getNextRevision() != null && !t.getNextRevision().isAfter(now.toLocalDate()))
                        || (t.getEstimatedRetention() != null && t.getEstimatedRetention() <= 60))
                .sorted(Comparator.comparing(t -> t.getEstimatedRetention() != null ? t.getEstimatedRetention() : 100))
                .toList();

        List<PracticeProblemResponse> queue = sessionPlanner.build(
                userId, scored, attemptsBySlug, revisionTopics, profile, 10);

        List<String> revisitTopics = revisionTopics.stream().map(Topic::getTitle).limit(4).toList();

        return new PracticeQueueResponse(
                profile.name().toLowerCase(),
                coldStartService.planMessage(userId, profile),
                queue,
                revisitTopics);
    }

    @Transactional
    public ProblemAttemptResponse submitAttempt(ProblemAttemptRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String outcome = request.getOutcome() != null ? request.getOutcome().toUpperCase() : null;
        if (!VALID_OUTCOMES.contains(outcome)) {
            throw new BadRequestException("Outcome must be one of " + VALID_OUTCOMES);
        }
        if (request.getProblemSlug() == null || request.getProblemSlug().isBlank()) {
            throw new BadRequestException("problemSlug is required");
        }

        User user = userRepository.findById(userId).orElse(null);
        ProblemAttempt attempt = new ProblemAttempt();
        attempt.setUser(user);
        attempt.setProblemTitle(request.getProblemTitle());
        attempt.setProblemSlug(request.getProblemSlug());
        attempt.setDifficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM");
        attempt.setTopicTagSlug(request.getTopicTagSlug());
        attempt.setTopicTagName(request.getTopicTagName());
        attempt.setOutcome(outcome);
        attempt.setHintsUsed(request.getHintsUsed() != null ? request.getHintsUsed() : 0);
        attempt.setTimeTakenSeconds(request.getTimeTakenSeconds());
        attempt.setQuality(masteryService.qualityFrom(outcome, attempt.getHintsUsed(), attempt.getTimeTakenSeconds()));
        attempt.setAttemptedAt(TimezoneUtil.now(user));
        snapshotSignals(attempt, request, userId);
        attempt = problemAttemptRepository.save(attempt);

        List<Topic> matched = matchTopics(userId, request.getTopicTagSlug(), request.getTopicTagName(), request.getProblemTitle());
        List<String> updatedTitles = new ArrayList<>();
        for (Topic topic : matched) {
            masteryService.apply(topic, outcome, attempt.getHintsUsed(), attempt.getTimeTakenSeconds());
            boolean solved = "SOLVED".equals(outcome) || "PARTIAL".equals(outcome);
            double oldSkill = topic.getSkillRating() != null ? topic.getSkillRating() : SkillRatingService.INITIAL_RATING;
            double newSkill = skillRatingService.applyResult(
                    oldSkill, attempt.getDifficulty(), solved, topic.getAttemptsTotal());
            topic.setSkillRating(newSkill);
            forgettingCurveService.strengthen(topic, "SOLVED".equals(outcome) ? 1.0 : ("PARTIAL".equals(outcome) ? 0.5 : 0.0));
            forgettingCurveService.refreshTopicRetention(topic);
            topicRepository.save(topic);
            updatedTitles.add(topic.getTitle());
            propagateGraph(userId, topic, outcome);
        }

        String feedback = buildFeedback(outcome, request.getProblemTitle(), matched);
        recommendationService.completeRecommendationsForProblem(userId, request.getProblemSlug(), outcome);
        log.info("Attempt submitted: {} {} ({} matching topics) for user {}",
                outcome, request.getProblemSlug(), matched.size(), userId);
        return new ProblemAttemptResponse(ProblemAttemptDto.from(attempt), updatedTitles, feedback);
    }

    @Transactional(readOnly = true)
    public List<ProblemAttemptSummary> getAttemptHistory(int limit) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return problemAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId, PageRequest.of(0, Math.min(50, Math.max(1, limit))))
                .stream()
                .map(ProblemAttemptSummary::from)
                .toList();
    }

    private void snapshotSignals(ProblemAttempt attempt, ProblemAttemptRequest request, UUID userId) {
        if (request.getTopicTagSlug() == null || request.getTopicTagSlug().isBlank()) {
            return;
        }
        try {
            ProblemScorer.ScoringContext ctx = problemScorer.context(userId);
            ProblemScorer.ScoreBreakdown breakdown = problemScorer.breakdown(ctx,
                    new ProblemLoader.ProblemEntry(request.getProblemTitle(), request.getProblemSlug(),
                            attempt.getDifficulty()),
                    request.getTopicTagSlug());
            attempt.setPredictedScore(breakdown.total());
            attempt.setSignalsJson(OBJECT_MAPPER.writeValueAsString(breakdown.items()));
        } catch (Exception e) {
            log.warn("Signal snapshot failed for {}: {}", request.getProblemSlug(), e.getMessage());
        }
    }

    private Map<String, SessionPlanner.AttemptCounts> attemptsBySlug(ProblemScorer.ScoringContext ctx) {
        Map<String, SessionPlanner.AttemptCounts> map = new HashMap<>();
        for (ProblemAttempt a : ctx.attempts()) {
            SessionPlanner.AttemptCounts counts = map.computeIfAbsent(a.getProblemSlug(),
                    k -> new SessionPlanner.AttemptCounts(0, 0));
            map.put(a.getProblemSlug(), new SessionPlanner.AttemptCounts(counts.attempts() + 1,
                    counts.solved() + ("SOLVED".equals(a.getOutcome()) || "PARTIAL".equals(a.getOutcome()) ? 1 : 0)));
        }
        return map;
    }

    private List<Topic> matchTopics(UUID userId, String tagSlug, String tagName, String problemTitle) {
        List<Topic> all = topicRepository.findByUserId(userId, PageRequest.of(0, 500));
        return all.stream()
                .filter(t -> matches(t.getTitle(), tagSlug) || matches(t.getTitle(), tagName) || matches(t.getTitle(), slugify(problemTitle)))
                .toList();
    }

    private void propagateGraph(UUID userId, Topic topic, String outcome) {
        String concept = knowledgeGraphService.matchConcept(topic.getTitle());
        int delta = switch (outcome) {
            case "SOLVED" -> 15;
            case "PARTIAL" -> 6;
            case "FAILED" -> -8;
            default -> 0;
        };
        if (concept != null) {
            knowledgeGraphService.propagateBoost(userId, concept, delta);
        }
    }

    private String buildFeedback(String outcome, String title, List<Topic> matched) {
        if (matched.isEmpty()) {
            return outcome + " " + title + " recorded. No matching topic yet — add one to link your progress.";
        }
        StringBuilder sb = new StringBuilder(outcome + " " + title + " recorded. Updated: ");
        sb.append(matched.stream().map(Topic::getTitle).collect(Collectors.joining(", ")));
        return sb.toString();
    }

    private boolean matches(String topicTitle, String candidate) {
        if (topicTitle == null || candidate == null) return false;
        String c = candidate.replace("-", " ").toLowerCase().trim();
        String t = topicTitle.toLowerCase();
        return t.contains(c) || c.contains(t);
    }

    private String slugify(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }
}
