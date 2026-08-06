package com.forge.practice.service;

import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.recommendation.service.CandidatePoolService;
import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SessionPlanner {

    public static final String SEGMENT_WARMUP = "WARMUP";
    public static final String SEGMENT_REINFORCE = "REINFORCE";
    public static final String SEGMENT_CHALLENGE = "CHALLENGE";
    public static final String SEGMENT_REVISION = "REVISION";

    private final SkillRatingService skillRatingService;

    public SessionPlanner(SkillRatingService skillRatingService) {
        this.skillRatingService = skillRatingService;
    }

    public record AttemptCounts(int attempts, int solved) {}

    private record Pick(CandidatePoolService.Candidate candidate, String segment, String reason) {}

    public List<PracticeProblemResponse> build(
            UUID userId,
            List<CandidatePoolService.Candidate> scored,
            Map<String, AttemptCounts> attemptsBySlug,
            List<Topic> revisionTopics,
            ColdStartService.Profile profile,
            int cap) {

        List<CandidatePoolService.Candidate> byScore = scored.stream()
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();

        Map<String, String> revisionReasonBySlug = revisionReasonsBySlug(byScore, revisionTopics);
        int revisionSlots = Math.min(2, revisionReasonBySlug.size());
        int warmupSlots = profile == ColdStartService.Profile.BEGINNER ? 2 : 1;
        int challengeSlots = 1;

        List<PracticeProblemResponse> result = new ArrayList<>();
        Set<String> used = new HashSet<>();

        while (result.size() < cap) {
            Pick pick = nextBest(byScore, used, revisionSlots, revisionReasonBySlug, warmupSlots, challengeSlots, profile);
            if (pick == null) break;
            used.add(pick.candidate().problem().getTitleSlug());
            switch (pick.segment()) {
                case SEGMENT_REVISION -> revisionSlots--;
                case SEGMENT_WARMUP -> warmupSlots--;
                case SEGMENT_CHALLENGE -> challengeSlots--;
                default -> { }
            }
            AttemptCounts counts = attemptsBySlug.getOrDefault(
                    pick.candidate().problem().getTitleSlug(), new AttemptCounts(0, 0));
            result.add(toResponse(pick.candidate(), pick.segment(), pick.reason(), counts));
        }

        return result;
    }

    /**
     * Marginal-gain selection: each step picks the highest-score unused candidate that fits a
     * remaining segment slot, instead of locking segment passes in a fixed order.
     */
    private Pick nextBest(List<CandidatePoolService.Candidate> byScore, Set<String> used,
                          int revisionSlots, Map<String, String> revisionReasonBySlug,
                          int warmupSlots, int challengeSlots, ColdStartService.Profile profile) {
        for (CandidatePoolService.Candidate candidate : byScore) {
            if (used.contains(candidate.problem().getTitleSlug())) continue;
            String slug = candidate.problem().getTitleSlug();
            if (revisionSlots > 0 && revisionReasonBySlug.containsKey(slug)) {
                return new Pick(candidate, SEGMENT_REVISION, revisionReasonBySlug.get(slug));
            }
            if (warmupSlots > 0 && "EASY".equalsIgnoreCase(candidate.problem().getDifficulty())) {
                return new Pick(candidate, SEGMENT_WARMUP, "Warm-up: build confidence before the heavy lifts.");
            }
            if (challengeSlots > 0 && challengeEligible(candidate, profile)) {
                return new Pick(candidate, SEGMENT_CHALLENGE, "Stretch: one problem past your comfort zone to grow.");
            }
            return new Pick(candidate, SEGMENT_REINFORCE, "Targeted practice on your weakest signals.");
        }
        return null;
    }

    private boolean challengeEligible(CandidatePoolService.Candidate candidate, ColdStartService.Profile profile) {
        String difficulty = candidate.problem().getDifficulty();
        boolean hardish = "HARD".equalsIgnoreCase(difficulty) || "MEDIUM".equalsIgnoreCase(difficulty);
        if (!hardish) return false;
        return !(profile == ColdStartService.Profile.BEGINNER && "HARD".equalsIgnoreCase(difficulty));
    }

    private Map<String, String> revisionReasonsBySlug(List<CandidatePoolService.Candidate> scored,
                                                      List<Topic> revisionTopics) {
        Map<String, String> reasons = new LinkedHashMap<>();
        for (Topic topic : revisionTopics) {
            for (CandidatePoolService.Candidate candidate : scored) {
                if (reasons.containsKey(candidate.problem().getTitleSlug())) continue;
                if (!matches(candidate.tagSlug(), topic.getTitle())) continue;
                reasons.put(candidate.problem().getTitleSlug(),
                        topic.getTitle() + " needs reinforcement — retention is " + round(topic.getEstimatedRetention()) + "%.");
            }
        }
        return reasons;
    }

    private PracticeProblemResponse toResponse(CandidatePoolService.Candidate sp, String segment, String reason,
                                               AttemptCounts counts) {
        return new PracticeProblemResponse(
                sp.problem().getTitle(),
                sp.problem().getTitleSlug(),
                sp.problem().getDifficulty(),
                sp.tagSlug(),
                reason,
                segment,
                sp.score(),
                sp.breakdown() != null ? sp.breakdown().items() : List.of(),
                counts.attempts(),
                counts.solved());
    }

    private boolean matches(String tagSlug, String topicTitle) {
        if (tagSlug == null || topicTitle == null) return false;
        String searchName = tagSlug.replace("-", " ").toLowerCase();
        String title = topicTitle.toLowerCase();
        return title.contains(searchName) || searchName.contains(title);
    }

    private String round(Double value) {
        if (value == null) return "100";
        return String.valueOf(Math.round(value));
    }
}
