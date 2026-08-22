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
        InputStream is = openStream();
        if (is == null) {
            throw new IllegalStateException("problems.json not found on the classpath; refusing to start with an empty practice queue");
        }
        load(is);
    }

    InputStream openStream() {
        return getClass().getResourceAsStream("/problems.json");
    }

    void load(InputStream is) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<Map<String, String>>> raw = mapper.readValue(is, new TypeReference<>() {});
            Set<String> seenSlugs = new HashSet<>();
            for (var entry : raw.entrySet()) {
                String tagSlug = entry.getKey();
                List<ProblemEntry> tagProblems = new ArrayList<>();
                for (Map<String, String> p : entry.getValue()) {
                    if (isBlank(p.get("title")) || isBlank(p.get("titleSlug")) || isBlank(p.get("difficulty"))) {
                        throw new IllegalStateException("problems.json contains an incomplete problem entry under tag '" + tagSlug + "'");
                    }
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
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse problems.json", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
