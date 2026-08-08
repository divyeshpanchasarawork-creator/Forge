package com.forge.roadmap.service;

import com.forge.common.util.ReadinessCalculator;
import com.forge.common.util.SecurityUtils;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.roadmap.dto.RoadmapAnalysisResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final UserRepository userRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;
    private final LeetCodeTagStatRepository tagStatRepository;
    private final TopicRepository topicRepository;

    public RoadmapAnalysisResponse getAnalysis() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        LeetCodeSnapshot snapshot = snapshotRepository.findByUserId(userId).orElse(null);
        List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);
        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 100));

        int totalSolved = snapshot != null && snapshot.getTotalSolved() != null ? snapshot.getTotalSolved() : 0;
        int easy = snapshot != null && snapshot.getEasySolved() != null ? snapshot.getEasySolved() : 0;
        int medium = snapshot != null && snapshot.getMediumSolved() != null ? snapshot.getMediumSolved() : 0;
        int hard = snapshot != null && snapshot.getHardSolved() != null ? snapshot.getHardSolved() : 0;
        int targetLevel = user.getTargetLevel() != null ? user.getTargetLevel() : 5;
        int streak = snapshot != null && snapshot.getStreak() != null ? snapshot.getStreak() : 0;
        int readiness = ReadinessCalculator.computeReadinessScore(targetLevel, allTopics, snapshot);

        List<RoadmapAnalysisResponse.TagInfo> strongTags = tagStats.stream()
                .filter(ts -> ts.getProblemsSolved() != null && ts.getProblemsSolved() >= 10)
                .sorted(Comparator.comparingInt(LeetCodeTagStat::getProblemsSolved).reversed())
                .limit(4)
                .map(ts -> new RoadmapAnalysisResponse.TagInfo(
                        ts.getTagName(), ts.getTagSlug(), ts.getProblemsSolved(),
                        topicConfidence(allTopics, ts.getTagName())))
                .toList();

        List<RoadmapAnalysisResponse.TagInfo> weakTags = tagStats.stream()
                .filter(ts -> ts.getProblemsSolved() != null && ts.getProblemsSolved() < 5 && ts.getProblemsSolved() > 0)
                .sorted(Comparator.comparingInt(LeetCodeTagStat::getProblemsSolved))
                .limit(4)
                .map(ts -> new RoadmapAnalysisResponse.TagInfo(
                        ts.getTagName(), ts.getTagSlug(), ts.getProblemsSolved(),
                        topicConfidence(allTopics, ts.getTagName())))
                .toList();

        String focusArea = weakTags.isEmpty()
                ? (strongTags.isEmpty() ? "Getting started" : "Maintain momentum")
                : weakTags.getFirst().name();

        int targetTotal = ReadinessCalculator.getTargetTotal(targetLevel);
        int toGo = Math.max(0, targetTotal - totalSolved);
        int nextMilestone = ((totalSolved / 50) + 1) * 50;
        String nextMilestoneStr = nextMilestone + " problems";
        String estimatedTime = estimateTime(targetLevel, totalSolved, medium, hard, streak);

        String difficultySplit = recommendSplit(targetLevel, easy, medium, hard);

        String paragraph = buildParagraph(user, snapshot, tagStats, allTopics,
                targetLevel, totalSolved, easy, medium, hard,
                streak, readiness, focusArea, strongTags, weakTags, toGo, nextMilestone);

        return new RoadmapAnalysisResponse(
                paragraph, targetLevel, focusArea, estimatedTime,
                strongTags, weakTags, nextMilestoneStr, readiness, difficultySplit);
    }

    private String buildParagraph(User user, LeetCodeSnapshot snapshot,
                                  List<LeetCodeTagStat> tagStats, List<Topic> allTopics,
                                  int targetLevel, int totalSolved, int easy, int medium, int hard,
                                  int streak, int readiness, String focusArea,
                                  List<RoadmapAnalysisResponse.TagInfo> strongTags,
                                  List<RoadmapAnalysisResponse.TagInfo> weakTags,
                                  int toGo, int nextMilestone) {
        StringBuilder sb = new StringBuilder();
        sb.append("You're at Level ").append(targetLevel).append(" with ")
                .append(totalSolved).append(" problems solved");

        if (totalSolved > 0) {
            sb.append(" (").append(easy).append("E / ").append(medium).append("M / ").append(hard).append("H)");
        }
        sb.append(". ");

        if (!strongTags.isEmpty()) {
            String strongNames = strongTags.stream()
                    .map(RoadmapAnalysisResponse.TagInfo::name)
                    .collect(Collectors.joining(", "));
            sb.append("Your strengths are in ").append(strongNames).append(". ");
        }

        if (!weakTags.isEmpty()) {
            String weakNames = weakTags.stream()
                    .map(RoadmapAnalysisResponse.TagInfo::name)
                    .collect(Collectors.joining(", "));
            sb.append("Focus on building depth in ").append(weakNames).append(". ");
        }

        if (toGo > 0) {
            sb.append("To reach Level ").append(targetLevel)
                    .append(", you need ").append(toGo).append(" more problems. ");
        }

        if (streak > 0) {
            sb.append("Your ").append(streak).append("-day streak shows great consistency");
            if (readiness < 50) {
                sb.append(" — now channel that into your weak areas");
            }
            sb.append(". ");
        } else if (totalSolved > 0) {
            sb.append("Try to build a daily streak — consistency compounds. ");
        }

        sb.append("Your readiness score is ").append(readiness)
                .append("% — ").append(readiness >= 70 ? "you're on track!" :
                        readiness >= 40 ? "steady progress, keep going." :
                                "early days, every problem counts.");

        if (focusArea != null && !focusArea.isBlank() && !"Getting started".equals(focusArea)
                && !"Maintain momentum".equals(focusArea)) {
            sb.append(" Your next focus area: ").append(focusArea).append(".");
        }

        return sb.toString();
    }

    private String estimateTime(int targetLevel, int totalSolved, int medium, int hard, int streak) {
        int target = ReadinessCalculator.getTargetTotal(targetLevel);
        int remaining = Math.max(0, target - totalSolved);
        if (remaining == 0) return "On track for Level " + targetLevel;

        int dailyRate = streak > 7 ? 3 : (streak > 0 ? 2 : 1);
        int estDays = remaining / dailyRate;
        if (estDays <= 7) return "~" + estDays + " day(s)";
        if (estDays <= 30) return "~" + (estDays / 7) + " week(s)";
        return "~" + (estDays / 30) + " month(s)";
    }

    private String recommendSplit(int targetLevel, int easy, int medium, int hard) {
        if (targetLevel >= 7) return "2 Easy : 3 Medium : 5 Hard";
        if (targetLevel >= 4) return "3 Easy : 5 Medium : 2 Hard";
        return "5 Easy : 4 Medium : 1 Hard";
    }

    private int topicConfidence(List<Topic> allTopics, String tagName) {
        String search = tagName.toLowerCase();
        return allTopics.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(search)
                        || search.contains(t.getTitle().toLowerCase()))
                .findFirst()
                .map(t -> t.getConfidence() != null ? t.getConfidence() : 5)
                .orElse(5);
    }
}
