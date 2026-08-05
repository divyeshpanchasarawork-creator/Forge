package com.forge.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
public class ProblemLoader {

    private final Map<String, List<ProblemEntry>> problemsByTag = new HashMap<>();
    private final Map<String, String> tagBySlug = new HashMap<>();
    private final List<ProblemEntry> allProblems = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/problems.json");
            if (is == null) {
                log.warn("problems.json not found, practice queue will be empty");
                return;
            }
            Map<String, List<Map<String, String>>> raw = mapper.readValue(is, new TypeReference<>() {});
            Set<String> seenSlugs = new HashSet<>();
            for (var entry : raw.entrySet()) {
                String tagSlug = entry.getKey();
                List<ProblemEntry> tagProblems = new ArrayList<>();
                for (Map<String, String> p : entry.getValue()) {
                    ProblemEntry pe = new ProblemEntry(p.get("title"), p.get("titleSlug"), p.get("difficulty"));
                    tagProblems.add(pe);
                    tagBySlug.putIfAbsent(pe.getTitleSlug(), tagSlug);
                    if (seenSlugs.add(pe.getTitleSlug())) {
                        allProblems.add(pe);
                    }
                }
                problemsByTag.put(tagSlug, tagProblems);
            }
            log.info("Loaded {} curated problems across {} tags ({} unique)", allProblems.size(), problemsByTag.size(), seenSlugs.size());
        } catch (Exception e) {
            log.error("Failed to load problems.json: {}", e.getMessage());
        }
    }

    public List<ProblemEntry> getProblemsForTag(String tagSlug) {
        return problemsByTag.getOrDefault(tagSlug, List.of());
    }

    public List<ProblemEntry> getAllProblems() {
        return allProblems;
    }

    public Set<String> getAllTagSlugs() {
        return problemsByTag.keySet();
    }

    public String getTagSlugForProblem(String titleSlug) {
        return tagBySlug.get(titleSlug);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProblemEntry {
        private String title;
        private String titleSlug;
        private String difficulty;

        public ProblemEntry(String title, String titleSlug, String difficulty) {
            this.title = title;
            this.titleSlug = titleSlug;
            this.difficulty = difficulty;
        }
    }
}
