package com.forge.practice.service;

import com.forge.common.exception.BadRequestException;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SecurityUtils;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.MasteryService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.knowledge.service.KnowledgeGraphService;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.dto.PracticeQueueResponse;
import com.forge.practice.dto.ProblemAttemptRequest;
import com.forge.practice.dto.ProblemAttemptResponse;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
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
    private static final int MAX_CANDIDATES = 400;
    private static final List<String> STARTER_TAGS = List.of("array", "hash-table", "two-pointers", "string", "binary-search");

    private final LeetCodeTagStatRepository tagStatRepository;
    private final ProblemLoader problemLoader;
    private final ProblemScorer problemScorer;
    private final ProblemSuggestionRepository problemSuggestionRepository;
    private final ProblemAttemptRepository problemAttemptRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ColdStartService coldStartService;
    private final SessionPlanner sessionPlanner;
    private final MasteryService masteryService;
    private final SkillRatingService skillRatingService;
    private final ForgettingCurveService forgettingCurveService;
    private final KnowledgeGraphService knowledgeGraphService;

    public PracticeQueueResponse getPracticeQueue() {
        UUID userId = SecurityUtils.getCurrentUserId();

        if (coldStartService.needsSeed(userId)) {
            coldStartService.seedStarterTopics(userId);
        }
        ColdStartService.Profile profile = coldStartService.classify(userId);

        List<ProblemScorer.ScoredProblem> scored = scoreCandidatePool(userId);
        Map<String, SessionPlanner.AttemptCounts> attemptsBySlug = attemptsBySlug(userId);

        List<Topic> revisionTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 500)).getContent().stream()
                .filter(t -> (t.getNextRevision() != null && !t.getNextRevision().isAfter(LocalDateTime.now()))
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

        ProblemAttempt attempt = new ProblemAttempt();
        attempt.setUser(userRepository.getReferenceById(userId));
        attempt.setProblemTitle(request.getProblemTitle());
        attempt.setProblemSlug(request.getProblemSlug());
        attempt.setDifficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM");
        attempt.setTopicTagSlug(request.getTopicTagSlug());
        attempt.setTopicTagName(request.getTopicTagName());
        attempt.setOutcome(outcome);
        attempt.setHintsUsed(request.getHintsUsed() != null ? request.getHintsUsed() : 0);
        attempt.setTimeTakenSeconds(request.getTimeTakenSeconds());
        attempt.setQuality(masteryService.qualityFrom(outcome, attempt.getHintsUsed(), attempt.getTimeTakenSeconds()));
        attempt.setAttemptedAt(LocalDateTime.now());
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
        log.info("Attempt submitted: {} {} ({} matching topics) for user {}",
                outcome, request.getProblemSlug(), matched.size(), userId);
        return new ProblemAttemptResponse(attempt, updatedTitles, feedback);
    }

    public List<ProblemAttempt> getAttemptHistory(int limit) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return problemAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId, PageRequest.of(0, Math.min(50, Math.max(1, limit))));
    }

    private List<ProblemScorer.ScoredProblem> scoreCandidatePool(UUID userId) {
        List<ProblemScorer.ScoredProblem> scored = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        List<ProblemSuggestion> recSuggestions = problemSuggestionRepository.findByUserId(userId).stream()
                .filter(ps -> "RECOMMENDATION".equals(ps.getSource()))
                .toList();
        for (ProblemSuggestion ps : recSuggestions) {
            String tag = ps.getTopicTagSlug();
            ProblemLoader.ProblemEntry entry = new ProblemLoader.ProblemEntry(ps.getTitle(), ps.getTitleSlug(), ps.getDifficulty());
            if (seen.add(ps.getTitleSlug())) {
                scored.add(new ProblemScorer.ScoredProblem(entry, tag, problemScorer.score(userId, entry, tag),
                        problemScorer.breakdown(userId, entry, tag)));
            }
        }

        List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);
        List<String> candidateTags;
        if (tagStats.isEmpty()) {
            candidateTags = STARTER_TAGS;
        } else {
            candidateTags = tagStats.stream()
                    .filter(ts -> ts.getProblemsSolved() == null || ts.getProblemsSolved() < 5)
                    .map(LeetCodeTagStat::getTagSlug)
                    .filter(slug -> problemLoader.getProblemsForTag(slug) != null
                            && !problemLoader.getProblemsForTag(slug).isEmpty())
                    .collect(Collectors.toList());
            if (candidateTags.isEmpty()) {
                candidateTags = tagStats.stream().map(LeetCodeTagStat::getTagSlug).toList();
            }
        }

        for (String tagSlug : candidateTags) {
            if (scored.size() >= MAX_CANDIDATES) break;
            String tagName = tagStats.stream()
                    .filter(ts -> ts.getTagSlug().equals(tagSlug))
                    .findFirst().map(LeetCodeTagStat::getTagName)
                    .orElse(tagSlug);
            for (ProblemLoader.ProblemEntry candidate : problemLoader.getProblemsForTag(tagSlug)) {
                if (scored.size() >= MAX_CANDIDATES) break;
                if (!seen.add(candidate.getTitleSlug())) continue;
                scored.add(new ProblemScorer.ScoredProblem(candidate, tagSlug, problemScorer.score(userId, candidate, tagSlug),
                        problemScorer.breakdown(userId, candidate, tagSlug)));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored;
    }

    private Map<String, SessionPlanner.AttemptCounts> attemptsBySlug(UUID userId) {
        Map<String, SessionPlanner.AttemptCounts> map = new HashMap<>();
        for (ProblemAttempt a : problemAttemptRepository.findByUserIdAll(userId)) {
            SessionPlanner.AttemptCounts counts = map.computeIfAbsent(a.getProblemSlug(),
                    k -> new SessionPlanner.AttemptCounts(0, 0));
            map.put(a.getProblemSlug(), new SessionPlanner.AttemptCounts(counts.attempts() + 1,
                    counts.solved() + ("SOLVED".equals(a.getOutcome()) || "PARTIAL".equals(a.getOutcome()) ? 1 : 0)));
        }
        return map;
    }

    private List<Topic> matchTopics(UUID userId, String tagSlug, String tagName, String problemTitle) {
        List<Topic> all = topicRepository.findByUserId(userId, PageRequest.of(0, 500)).getContent();
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
