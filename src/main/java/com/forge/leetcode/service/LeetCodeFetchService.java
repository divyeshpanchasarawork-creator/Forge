package com.forge.leetcode.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.exception.ServiceUnavailableException;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.TimezoneUtil;
import com.forge.leetcode.client.LeetCodeClient;
import com.forge.leetcode.dto.LeetCodeGraphQlResponse;
import com.forge.leetcode.dto.LeetCodeStatsResponse;
import com.forge.leetcode.dto.PendingSolveResponse;
import com.forge.leetcode.entity.ExternalSolve;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.leetcode.repository.ExternalSolveRepository;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.recommendation.service.RecommendationEngine;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeetCodeFetchService {

    private static final Map<UUID, Object> SYNC_LOCKS = new ConcurrentHashMap<>();

    private final LeetCodeClient leetCodeClient;
    private final LeetCodeSnapshotRepository snapshotRepository;
    private final LeetCodeTagStatRepository tagStatRepository;
    private final ProblemSuggestionRepository problemSuggestionRepository;
    private final ExternalSolveRepository externalSolveRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final LeetCodeTopicMapper topicMapper;
    private final RecommendationEngine recommendationEngine;
    private final ProblemLoader problemLoader;
    private final PlatformTransactionManager transactionManager;

    public LeetCodeStatsResponse syncUserProfile(UUID userId) {
        Object lock = SYNC_LOCKS.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                String lcUsername = user.getLeetcodeUsername();
                if (lcUsername == null || lcUsername.isBlank()) {
                    throw new BadRequestException("No LeetCode username set. Update your profile first.");
                }

                LeetCodeGraphQlResponse graphqlResponse = leetCodeClient.fetchUserProfile(lcUsername);
                if (graphqlResponse == null || graphqlResponse.getData() == null) {
                    throw new ServiceUnavailableException("Could not fetch LeetCode data for: " + lcUsername);
                }

                LeetCodeGraphQlResponse.Data data = graphqlResponse.getData();
                LeetCodeGraphQlResponse.MatchedUser matchedUser = data.getMatchedUser();
                if (matchedUser == null) {
                    throw new ResourceNotFoundException("LeetCode user", "username", lcUsername);
                }
                if (matchedUser.getTagProblemCounts() == null) {
                    throw new ServiceUnavailableException(
                            "LeetCode response is missing tag problem counts; aborting sync to protect existing data");
                }

                LeetCodeStatsResponse response = writeSyncData(userId, lcUsername, data, matchedUser);
                generateRecommendationsInOwnTransaction(userId);
                return response;
            } finally {
                SYNC_LOCKS.remove(userId, lock);
            }
        }
    }

    private LeetCodeStatsResponse writeSyncData(UUID userId, String lcUsername,
                                                LeetCodeGraphQlResponse.Data data,
                                                LeetCodeGraphQlResponse.MatchedUser matchedUser) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        return txTemplate.execute(status -> {
            LeetCodeSnapshot snapshot = saveSnapshot(userId, data, matchedUser);
            saveTagStats(userId, matchedUser);
            User managedUser = userRepository.getReferenceById(userId);
            syncTopicsFromTags(managedUser, matchedUser);
            fetchAndSaveProblemSuggestions(managedUser, matchedUser);
            upsertExternalSolves(managedUser, data.getRecentAcSubmissionList());
            log.info("LeetCode sync complete for user: {} (solved: {})", lcUsername, snapshot.getTotalSolved());
            return toStatsResponse(snapshot, userId);
        });
    }

    private void generateRecommendationsInOwnTransaction(UUID userId) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> recommendationEngine.generateForUser(userId, true));
    }

    /**
     * Records problems recently solved on LeetCode that are not yet known to Forge. Rows are
     * deduped by (user, slug); logging the solve through the practice flow flips {@code logged}
     * so it is never surfaced twice. Quality/mastery stay untouched until the user confirms.
     */
    private void upsertExternalSolves(User user, List<LeetCodeGraphQlResponse.RecentSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) return;
        java.time.ZoneId zone = TimezoneUtil.resolve(user);
        int created = 0;
        for (LeetCodeGraphQlResponse.RecentSubmission submission : submissions) {
            String slug = submission.getTitleSlug();
            if (slug == null || slug.isBlank()
                    || externalSolveRepository.existsByUserIdAndTitleSlug(user.getId(), slug)) {
                continue;
            }
            ExternalSolve solve = new ExternalSolve();
            solve.setUser(user);
            solve.setTitle(submission.getTitle());
            solve.setTitleSlug(slug);
            solve.setSolvedAt(parseSolvedAt(submission.getTimestamp(), zone));
            solve.setLogged(false);
            externalSolveRepository.save(solve);
            created++;
        }
        if (created > 0) {
            log.info("Detected {} new external solves for user {}", created, user.getId());
        }
    }

    private java.time.LocalDateTime parseSolvedAt(String epochSeconds, java.time.ZoneId zone) {
        try {
            return java.time.Instant.ofEpochSecond(Long.parseLong(epochSeconds)).atZone(zone).toLocalDateTime();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Unlogged external solves that exist in the curated library — surfaced to the user as
     * one-tap logging candidates. Solves outside the library are not offered.
     */
    @Transactional(readOnly = true)
    public List<PendingSolveResponse> getPendingSolves(UUID userId) {
        List<PendingSolveResponse> result = new ArrayList<>();
        for (ExternalSolve solve : externalSolveRepository.findByUserIdAndLoggedFalseOrderBySolvedAtDesc(userId)) {
            String tagSlug = problemLoader.getTagSlugForProblem(solve.getTitleSlug());
            if (tagSlug == null) continue;
            ProblemLoader.ProblemEntry problem = problemLoader.getProblemsForTag(tagSlug).stream()
                    .filter(p -> p.getTitleSlug().equals(solve.getTitleSlug()))
                    .findFirst().orElse(null);
            if (problem == null) continue;
            result.add(new PendingSolveResponse(solve.getId(), solve.getTitle(), solve.getTitleSlug(),
                    problem.getDifficulty(), tagSlug, solve.getSolvedAt()));
        }
        return result;
    }

    @Transactional
    public LeetCodeStatsResponse getLatestStats(UUID userId) {
        LeetCodeSnapshot snapshot = snapshotRepository.findByUserId(userId).orElse(null);
        if (snapshot == null) return null;
        return toStatsResponse(snapshot, userId);
    }

    private LeetCodeSnapshot saveSnapshot(UUID userId, LeetCodeGraphQlResponse.Data data, LeetCodeGraphQlResponse.MatchedUser matchedUser) {
        User user = userRepository.getReferenceById(userId);
        LeetCodeSnapshot snapshot = snapshotRepository.findByUserId(userId).orElse(new LeetCodeSnapshot());
        snapshot.setUser(user);

        if (matchedUser.getSubmitStatsGlobal() != null && matchedUser.getSubmitStatsGlobal().getAcSubmissionNum() != null) {
            for (LeetCodeGraphQlResponse.DifficultyCount dc : matchedUser.getSubmitStatsGlobal().getAcSubmissionNum()) {
                switch (dc.getDifficulty()) {
                    case "All" -> snapshot.setTotalSolved(dc.getCount());
                    case "Easy" -> snapshot.setEasySolved(dc.getCount());
                    case "Medium" -> snapshot.setMediumSolved(dc.getCount());
                    case "Hard" -> snapshot.setHardSolved(dc.getCount());
                }
            }
        }

        if (matchedUser.getProblemsSolvedBeatsStats() != null) {
            for (LeetCodeGraphQlResponse.BeatsStat bs : matchedUser.getProblemsSolvedBeatsStats()) {
                switch (bs.getDifficulty()) {
                    case "Easy" -> snapshot.setEasyBeatsPct(bs.getPercentage());
                    case "Medium" -> snapshot.setMediumBeatsPct(bs.getPercentage());
                    case "Hard" -> snapshot.setHardBeatsPct(bs.getPercentage());
                }
            }
        }

        if (matchedUser.getUserCalendar() != null) {
            LeetCodeGraphQlResponse.UserCalendar cal = matchedUser.getUserCalendar();
            snapshot.setStreak(cal.getStreak() != null ? cal.getStreak() : 0);
            snapshot.setTotalActiveDays(cal.getTotalActiveDays() != null ? cal.getTotalActiveDays() : 0);
        }

        if (data.getUserContestRanking() != null) {
            LeetCodeGraphQlResponse.UserContestRanking cr = data.getUserContestRanking();
            snapshot.setContestRating(cr.getRating());
            snapshot.setContestRanking(cr.getGlobalRanking());
            snapshot.setContestAttendedCount(cr.getAttendedContestsCount() != null ? cr.getAttendedContestsCount() : 0);
        }

        snapshot.setLastSyncedAt(TimezoneUtil.now(user));
        return snapshotRepository.save(snapshot);
    }

    private void saveTagStats(UUID userId, LeetCodeGraphQlResponse.MatchedUser matchedUser) {
        User user = userRepository.getReferenceById(userId);

        List<LeetCodeTagStat> allTags = new ArrayList<>();

        if (matchedUser.getTagProblemCounts() != null) {
            addTagsForLevel(allTags, user, matchedUser.getTagProblemCounts().getFundamental(), "fundamental");
            addTagsForLevel(allTags, user, matchedUser.getTagProblemCounts().getIntermediate(), "intermediate");
            addTagsForLevel(allTags, user, matchedUser.getTagProblemCounts().getAdvanced(), "advanced");
        }

        if (allTags.isEmpty() && !tagStatRepository.findByUserId(userId).isEmpty()) {
            throw new ServiceUnavailableException(
                    "LeetCode sync produced no tag stats while existing stats are present; aborting to protect existing data");
        }

        tagStatRepository.deleteByUserId(userId);
        tagStatRepository.flush();

        tagStatRepository.saveAll(allTags);
    }

    private void addTagsForLevel(List<LeetCodeTagStat> allTags, User user, List<LeetCodeGraphQlResponse.TagCount> tags, String level) {
        if (tags == null) return;
        for (LeetCodeGraphQlResponse.TagCount tag : tags) {
            if (tag.getProblemsSolved() > 0) {
                LeetCodeTagStat stat = new LeetCodeTagStat();
                stat.setUser(user);
                stat.setTagName(tag.getTagName());
                stat.setTagSlug(tag.getTagSlug());
                stat.setProblemsSolved(tag.getProblemsSolved());
                stat.setSkillLevel(level);
                allTags.add(stat);
            }
        }
    }

    private void syncTopicsFromTags(User user, LeetCodeGraphQlResponse.MatchedUser matchedUser) {
        List<LeetCodeGraphQlResponse.TagCount> allTags = new ArrayList<>();
        if (matchedUser.getTagProblemCounts() != null) {
            if (matchedUser.getTagProblemCounts().getFundamental() != null) allTags.addAll(matchedUser.getTagProblemCounts().getFundamental());
            if (matchedUser.getTagProblemCounts().getIntermediate() != null) allTags.addAll(matchedUser.getTagProblemCounts().getIntermediate());
            if (matchedUser.getTagProblemCounts().getAdvanced() != null) allTags.addAll(matchedUser.getTagProblemCounts().getAdvanced());
        }

        List<Topic> topics = topicMapper.mapToTopics(user, allTags, "mixed");
        topicRepository.saveAll(topics);
    }

    private void fetchAndSaveProblemSuggestions(User user, LeetCodeGraphQlResponse.MatchedUser matchedUser) {
        UUID userId = user.getId();
        List<LeetCodeGraphQlResponse.TagCount> allTags = new ArrayList<>();
        if (matchedUser.getTagProblemCounts() != null) {
            if (matchedUser.getTagProblemCounts().getFundamental() != null) allTags.addAll(matchedUser.getTagProblemCounts().getFundamental());
            if (matchedUser.getTagProblemCounts().getIntermediate() != null) allTags.addAll(matchedUser.getTagProblemCounts().getIntermediate());
            if (matchedUser.getTagProblemCounts().getAdvanced() != null) allTags.addAll(matchedUser.getTagProblemCounts().getAdvanced());
        }

        List<String> weakTagSlugs = allTags.stream()
                .filter(t -> t.getProblemsSolved() < 5 && t.getProblemsSolved() > 0)
                .map(LeetCodeGraphQlResponse.TagCount::getTagSlug)
                .toList();

        problemSuggestionRepository.deleteByUserIdAndSource(userId, "WEAK_TAG");

        if (weakTagSlugs.isEmpty()) {
            log.debug("No weak tags to generate problem suggestions for user {}", userId);
            return;
        }

        List<ProblemSuggestion> suggestions = new ArrayList<>();
        for (String tagSlug : weakTagSlugs) {
            List<ProblemLoader.ProblemEntry> problems = problemLoader.getProblemsForTag(tagSlug);
            String tagName = allTags.stream()
                    .filter(t -> t.getTagSlug().equals(tagSlug))
                    .findFirst().map(LeetCodeGraphQlResponse.TagCount::getTagName)
                    .orElse(tagSlug);
            int count = 0;
            for (ProblemLoader.ProblemEntry p : problems) {
                if (count >= 3) break;
                boolean exists = suggestions.stream()
                        .anyMatch(s -> s.getTitleSlug().equals(p.getTitleSlug()));
                if (!exists) {
                    ProblemSuggestion suggestion = new ProblemSuggestion();
                    suggestion.setUser(user);
                    suggestion.setTitle(p.getTitle());
                    suggestion.setTitleSlug(p.getTitleSlug());
                    suggestion.setDifficulty(p.getDifficulty());
                    suggestion.setTopicTagSlug(tagSlug);
                    suggestion.setTopicTagName(tagName);
                    suggestion.setSource("WEAK_TAG");
                    suggestions.add(suggestion);
                    count++;
                }
            }
        }

        if (!suggestions.isEmpty()) {
            problemSuggestionRepository.flush();
            problemSuggestionRepository.saveAll(suggestions);
            log.info("Saved {} problem suggestions for {} weak tags (user {})", suggestions.size(), weakTagSlugs.size(), userId);
        } else {
            log.debug("No problem suggestions generated for user {} (no matching problems in bundle)", userId);
        }
    }

    @Transactional
    public void refreshProblemSuggestions(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);

        List<String> weakTagSlugs = tagStats.stream()
                .filter(ts -> ts.getProblemsSolved() < 5 && ts.getProblemsSolved() > 0)
                .map(LeetCodeTagStat::getTagSlug)
                .toList();

        problemSuggestionRepository.deleteByUserIdAndSource(userId, "WEAK_TAG");

        if (weakTagSlugs.isEmpty()) {
            log.debug("No weak tags to refresh problem suggestions for user {}", userId);
            return;
        }

        List<ProblemSuggestion> suggestions = new ArrayList<>();
        for (String tagSlug : weakTagSlugs) {
            List<ProblemLoader.ProblemEntry> problems = problemLoader.getProblemsForTag(tagSlug);
            String tagName = tagStats.stream()
                    .filter(ts -> ts.getTagSlug().equals(tagSlug))
                    .findFirst().map(LeetCodeTagStat::getTagName)
                    .orElse(tagSlug);
            int count = 0;
            for (ProblemLoader.ProblemEntry p : problems) {
                if (count >= 3) break;
                boolean exists = suggestions.stream()
                        .anyMatch(s -> s.getTitleSlug().equals(p.getTitleSlug()));
                if (!exists) {
                    ProblemSuggestion suggestion = new ProblemSuggestion();
                    suggestion.setUser(user);
                    suggestion.setTitle(p.getTitle());
                    suggestion.setTitleSlug(p.getTitleSlug());
                    suggestion.setDifficulty(p.getDifficulty());
                    suggestion.setTopicTagSlug(tagSlug);
                    suggestion.setTopicTagName(tagName);
                    suggestion.setSource("WEAK_TAG");
                    suggestions.add(suggestion);
                    count++;
                }
            }
        }

        if (!suggestions.isEmpty()) {
            problemSuggestionRepository.flush();
            problemSuggestionRepository.saveAll(suggestions);
            log.info("Refreshed {} problem suggestions for {} weak tags (user {})", suggestions.size(), weakTagSlugs.size(), userId);
        }
    }

    private LeetCodeStatsResponse toStatsResponse(LeetCodeSnapshot snapshot, UUID userId) {
        List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);
        List<LeetCodeStatsResponse.TagStat> tags = tagStats.stream()
                .map(ts -> new LeetCodeStatsResponse.TagStat(
                        ts.getTagName(), ts.getTagSlug(), ts.getProblemsSolved(), ts.getSkillLevel()))
                .toList();

        return new LeetCodeStatsResponse(
                snapshot.getTotalSolved(),
                snapshot.getEasySolved(),
                snapshot.getMediumSolved(),
                snapshot.getHardSolved(),
                snapshot.getEasyBeatsPct(),
                snapshot.getMediumBeatsPct(),
                snapshot.getHardBeatsPct(),
                snapshot.getRanking(),
                snapshot.getContestRating(),
                snapshot.getContestRanking(),
                snapshot.getContestAttendedCount(),
                snapshot.getStreak(),
                snapshot.getTotalActiveDays(),
                snapshot.getLastSyncedAt(),
                tags
        );
    }
}
