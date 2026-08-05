package com.forge.practice.service;

import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.recommendation.service.CandidatePoolService;
import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    public List<PracticeProblemResponse> build(
            UUID userId,
            List<CandidatePoolService.Candidate> scored,
            Map<String, AttemptCounts> attemptsBySlug,
            List<Topic> revisionTopics,
            ColdStartService.Profile profile,
            int cap) {

        List<PracticeProblemResponse> result = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();

        List<CandidatePoolService.Candidate> byScore = scored.stream()
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();

        addRevisionSegment(result, used, byScore, revisionTopics, attemptsBySlug, cap);
        addWarmupSegment(result, used, byScore, attemptsBySlug, cap, profile);
        addChallengeSegment(result, used, byScore, attemptsBySlug, cap, profile, userId);
        addReinforceSegment(result, used, byScore, attemptsBySlug, cap);

        return result;
    }

    private void addRevisionSegment(List<PracticeProblemResponse> result, Set<String> used,
                                    List<CandidatePoolService.Candidate> scored, List<Topic> revisionTopics,
                                    Map<String, AttemptCounts> attemptsBySlug, int cap) {
        if (revisionTopics.isEmpty()) return;
        int added = 0;
        for (Topic topic : revisionTopics) {
            if (result.size() >= cap || added >= 2) break;
            for (CandidatePoolService.Candidate sp : scored) {
                if (result.size() >= cap || added >= 2) break;
                if (!matches(sp.tagSlug(), topic.getTitle())) continue;
                if (used.add(sp.problem().getTitleSlug())) {
                    result.add(toResponse(sp, SEGMENT_REVISION,
                            topic.getTitle() + " needs reinforcement — retention is " + round(topic.getEstimatedRetention()) + "%.",
                            attemptsBySlug.getOrDefault(sp.problem().getTitleSlug(), new AttemptCounts(0, 0))));
                    added++;
                }
            }
        }
    }

    private void addWarmupSegment(List<PracticeProblemResponse> result, Set<String> used,
                                  List<CandidatePoolService.Candidate> byScore,
                                  Map<String, AttemptCounts> attemptsBySlug, int cap, ColdStartService.Profile profile) {
        int wanted = profile == ColdStartService.Profile.BEGINNER ? 2 : 1;
        int added = 0;
        for (CandidatePoolService.Candidate sp : byScore) {
            if (result.size() >= cap || added >= wanted) break;
            if (!"EASY".equalsIgnoreCase(sp.problem().getDifficulty())) continue;
            if (used.add(sp.problem().getTitleSlug())) {
                result.add(toResponse(sp, SEGMENT_WARMUP,
                        "Warm-up: build confidence before the heavy lifts.",
                        attemptsBySlug.getOrDefault(sp.problem().getTitleSlug(), new AttemptCounts(0, 0))));
                added++;
            }
        }
    }

    private void addChallengeSegment(List<PracticeProblemResponse> result, Set<String> used,
                                     List<CandidatePoolService.Candidate> byScore,
                                     Map<String, AttemptCounts> attemptsBySlug, int cap,
                                     ColdStartService.Profile profile, UUID userId) {
        int added = 0;
        for (CandidatePoolService.Candidate sp : byScore) {
            if (result.size() >= cap || added >= 1) break;
            boolean hardish = "HARD".equalsIgnoreCase(sp.problem().getDifficulty())
                    || "MEDIUM".equalsIgnoreCase(sp.problem().getDifficulty());
            if (!hardish) continue;
            if (profile == ColdStartService.Profile.BEGINNER && "HARD".equalsIgnoreCase(sp.problem().getDifficulty())) continue;
            if (used.add(sp.problem().getTitleSlug())) {
                result.add(toResponse(sp, SEGMENT_CHALLENGE,
                        "Stretch: one problem past your comfort zone to grow.",
                        attemptsBySlug.getOrDefault(sp.problem().getTitleSlug(), new AttemptCounts(0, 0))));
                added++;
            }
        }
    }

    private void addReinforceSegment(List<PracticeProblemResponse> result, Set<String> used,
                                     List<CandidatePoolService.Candidate> byScore,
                                     Map<String, AttemptCounts> attemptsBySlug, int cap) {
        for (CandidatePoolService.Candidate sp : byScore) {
            if (result.size() >= cap) break;
            if (used.add(sp.problem().getTitleSlug())) {
                result.add(toResponse(sp, SEGMENT_REINFORCE,
                        "Targeted practice on your weakest signals.",
                        attemptsBySlug.getOrDefault(sp.problem().getTitleSlug(), new AttemptCounts(0, 0))));
            }
        }
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
