package com.forge.leetcode.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.leetcode.client.LeetCodeClient;
import com.forge.leetcode.dto.LeetCodeGraphQlResponse;
import com.forge.leetcode.dto.LeetCodeStatsResponse;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeetCodeFetchService {

    private final LeetCodeClient leetCodeClient;
    private final LeetCodeSnapshotRepository snapshotRepository;
    private final LeetCodeTagStatRepository tagStatRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final LeetCodeTopicMapper topicMapper;

    @Transactional
    public LeetCodeStatsResponse syncUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String lcUsername = user.getLeetcodeUsername();
        if (lcUsername == null || lcUsername.isBlank()) {
            throw new IllegalStateException("No LeetCode username set. Update your profile first.");
        }

        LeetCodeGraphQlResponse graphqlResponse = leetCodeClient.fetchUserProfile(lcUsername);
        if (graphqlResponse == null || graphqlResponse.getData() == null) {
            throw new IllegalStateException("Could not fetch LeetCode data for: " + lcUsername);
        }

        LeetCodeGraphQlResponse.Data data = graphqlResponse.getData();
        LeetCodeGraphQlResponse.MatchedUser matchedUser = data.getMatchedUser();
        if (matchedUser == null) {
            throw new IllegalStateException("LeetCode user not found: " + lcUsername);
        }

        LeetCodeSnapshot snapshot = saveSnapshot(userId, data, matchedUser);
        saveTagStats(userId, matchedUser);
        syncTopicsFromTags(user, matchedUser);

        log.info("LeetCode sync complete for user: {} (solved: {})", lcUsername, snapshot.getTotalSolved());
        return toStatsResponse(snapshot, userId);
    }

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
            snapshot.setSubmissionCalendar(cal.getSubmissionCalendar());
        }

        if (data.getUserContestRanking() != null) {
            LeetCodeGraphQlResponse.UserContestRanking cr = data.getUserContestRanking();
            snapshot.setContestRating(cr.getRating());
            snapshot.setContestRanking(cr.getGlobalRanking());
            snapshot.setContestAttendedCount(cr.getAttendedContestsCount() != null ? cr.getAttendedContestsCount() : 0);
        }

        snapshot.setLastSyncedAt(LocalDateTime.now());
        return snapshotRepository.save(snapshot);
    }

    private void saveTagStats(UUID userId, LeetCodeGraphQlResponse.MatchedUser matchedUser) {
        User user = userRepository.getReferenceById(userId);
        tagStatRepository.deleteByUserId(userId);
        tagStatRepository.flush();

        List<LeetCodeTagStat> allTags = new ArrayList<>();

        addTagsForLevel(allTags, user, matchedUser.getTagProblemCounts() != null ? matchedUser.getTagProblemCounts().stream().flatMap(g -> g.getFundamental() != null ? g.getFundamental().stream() : java.util.stream.Stream.empty()).toList() : List.of(), "fundamental");
        addTagsForLevel(allTags, user, matchedUser.getTagProblemCounts() != null ? matchedUser.getTagProblemCounts().stream().flatMap(g -> g.getIntermediate() != null ? g.getIntermediate().stream() : java.util.stream.Stream.empty()).toList() : List.of(), "intermediate");
        addTagsForLevel(allTags, user, matchedUser.getTagProblemCounts() != null ? matchedUser.getTagProblemCounts().stream().flatMap(g -> g.getAdvanced() != null ? g.getAdvanced().stream() : java.util.stream.Stream.empty()).toList() : List.of(), "advanced");

        tagStatRepository.saveAll(allTags);
    }

    private void addTagsForLevel(List<LeetCodeTagStat> allTags, User user, List<LeetCodeGraphQlResponse.TagCount> tags, String level) {
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
            for (LeetCodeGraphQlResponse.TagProblemCountGroup group : matchedUser.getTagProblemCounts()) {
                if (group.getFundamental() != null) allTags.addAll(group.getFundamental());
                if (group.getIntermediate() != null) allTags.addAll(group.getIntermediate());
                if (group.getAdvanced() != null) allTags.addAll(group.getAdvanced());
            }
        }

        List<Topic> topics = topicMapper.mapToTopics(user, allTags, "mixed");
        topicRepository.saveAll(topics);
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
