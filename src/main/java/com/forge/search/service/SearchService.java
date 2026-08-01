package com.forge.search.service;

import com.forge.common.util.ProblemLoader;
import com.forge.search.dto.ProblemSearchItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_RESULTS = 8;

    private final ProblemLoader problemLoader;

    private volatile Map<String, List<String>> tagsBySlug;

    public List<ProblemSearchItem> searchProblems(String query) {
        String q = normalize(query);
        if (q.isBlank()) {
            return List.of();
        }

        Map<String, List<String>> tagMap = tagsBySlug();
        Map<String, Scored> bySlug = new LinkedHashMap<>();

        for (ProblemLoader.ProblemEntry p : problemLoader.getAllProblems()) {
            int score = scoreTitle(p.getTitle(), q);
            if (score > 0) {
                bySlug.merge(p.getTitleSlug(),
                        new Scored(p, score, tagMap.getOrDefault(p.getTitleSlug(), List.of())),
                        (a, b) -> a.score >= b.score ? a : b);
            }
        }

        for (String tag : problemLoader.getAllTagSlugs()) {
            String t = normalize(tag);
            if (t.contains(q) || q.contains(t)) {
                for (ProblemLoader.ProblemEntry p : problemLoader.getProblemsForTag(tag)) {
                    bySlug.merge(p.getTitleSlug(),
                            new Scored(p, 55, tagMap.getOrDefault(p.getTitleSlug(), List.of())),
                            (a, b) -> a.score >= b.score ? a : b);
                }
            }
        }

        return bySlug.values().stream()
                .sorted(Comparator.comparingInt((Scored s) -> s.score).reversed()
                        .thenComparing(s -> s.entry.getTitle()))
                .limit(MAX_RESULTS)
                .map(s -> new ProblemSearchItem(s.entry.getTitle(), s.entry.getTitleSlug(),
                        s.entry.getDifficulty(), s.tags))
                .collect(Collectors.toList());
    }

    private int scoreTitle(String title, String q) {
        String t = normalize(title);
        if (t.isEmpty() || t.length() < q.length()) {
            return 0;
        }
        if (t.equals(q)) {
            return 120;
        }
        if (t.startsWith(q)) {
            return 100;
        }
        for (String word : t.split(" ")) {
            if (word.startsWith(q)) {
                return 80;
            }
        }
        return t.contains(q) ? 60 : 0;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private Map<String, List<String>> tagsBySlug() {
        if (tagsBySlug == null) {
            Map<String, List<String>> map = new HashMap<>();
            for (String tag : problemLoader.getAllTagSlugs()) {
                for (ProblemLoader.ProblemEntry p : problemLoader.getProblemsForTag(tag)) {
                    map.computeIfAbsent(p.getTitleSlug(), k -> new ArrayList<>()).add(tag);
                }
            }
            tagsBySlug = map;
        }
        return tagsBySlug;
    }

    private record Scored(ProblemLoader.ProblemEntry entry, int score, List<String> tags) {
    }
}
