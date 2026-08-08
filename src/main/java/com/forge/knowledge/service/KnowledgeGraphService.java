package com.forge.knowledge.service;

import com.forge.knowledge.entity.ConceptPrerequisite;
import com.forge.knowledge.repository.ConceptPrerequisiteRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService implements ApplicationRunner {

    private static final Map<String, List<String>> CURATED_GRAPH = new LinkedHashMap<>();

    static {
        CURATED_GRAPH.put("hash-table", List.of("array"));
        CURATED_GRAPH.put("string", List.of("array"));
        CURATED_GRAPH.put("linked-list", List.of("array"));
        CURATED_GRAPH.put("stack", List.of("array"));
        CURATED_GRAPH.put("queue", List.of("linked-list"));
        CURATED_GRAPH.put("heap", List.of("array"));
        CURATED_GRAPH.put("monotonic-queue", List.of("queue", "sliding-window"));
        CURATED_GRAPH.put("monotonic-stack", List.of("stack"));
        CURATED_GRAPH.put("prefix-sum", List.of("array"));
        CURATED_GRAPH.put("segment-tree", List.of("array", "divide-and-conquer"));
        CURATED_GRAPH.put("fenwick-tree", List.of("prefix-sum", "binary-search"));
        CURATED_GRAPH.put("trie", List.of("string", "hash-table"));
        CURATED_GRAPH.put("design", List.of("array", "hash-table"));
        CURATED_GRAPH.put("two-pointers", List.of("array", "string"));
        CURATED_GRAPH.put("sliding-window", List.of("two-pointers", "hash-table"));
        CURATED_GRAPH.put("binary-search", List.of("array", "sorting"));
        CURATED_GRAPH.put("backtracking", List.of("recursion"));
        CURATED_GRAPH.put("dynamic-programming", List.of("recursion", "backtracking"));
        CURATED_GRAPH.put("greedy", List.of("sorting"));
        CURATED_GRAPH.put("bit-manipulation", List.of("math"));
        CURATED_GRAPH.put("trees", List.of("recursion", "linked-list"));
        CURATED_GRAPH.put("bst", List.of("trees", "binary-search"));
        CURATED_GRAPH.put("dfs", List.of("trees", "recursion"));
        CURATED_GRAPH.put("bfs", List.of("trees", "queue"));
        CURATED_GRAPH.put("graphs", List.of("trees", "hash-table"));
        CURATED_GRAPH.put("union-find", List.of("graphs", "dfs"));
        CURATED_GRAPH.put("topological-sort", List.of("graphs", "dfs"));
        CURATED_GRAPH.put("divide-and-conquer", List.of("recursion"));
        CURATED_GRAPH.put("sorting", List.of("array"));
        CURATED_GRAPH.put("recursion", List.of());
        CURATED_GRAPH.put("math", List.of());
    }

    private final ConceptPrerequisiteRepository prerequisiteRepository;
    private final TopicRepository topicRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedIfEmpty();
    }

    @Transactional
    public void seedIfEmpty() {
        if (prerequisiteRepository.count() > 0) {
            return;
        }
        List<ConceptPrerequisite> seeds = CURATED_GRAPH.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(pre -> {
                    ConceptPrerequisite cp = new ConceptPrerequisite();
                    cp.setConceptSlug(entry.getKey());
                    cp.setPrerequisiteSlug(pre);
                    return cp;
                }))
                .toList();
        prerequisiteRepository.saveAll(seeds);
        log.info("Seeded knowledge graph with {} prerequisite edges across {} concepts",
                seeds.size(), CURATED_GRAPH.size());
    }

    public List<String> getPrerequisites(String conceptSlug) {
        return prerequisiteRepository.findByConceptSlug(conceptSlug).stream()
                .map(ConceptPrerequisite::getPrerequisiteSlug)
                .collect(Collectors.toList());
    }

    public List<String> getDependents(String conceptSlug) {
        return prerequisiteRepository.findByPrerequisiteSlug(conceptSlug).stream()
                .map(ConceptPrerequisite::getConceptSlug)
                .collect(Collectors.toList());
    }

    public String matchConcept(String topicTitle) {
        if (topicTitle == null || topicTitle.isBlank()) {
            return null;
        }
        String normalized = topicTitle.toLowerCase().trim();
        for (String slug : CURATED_GRAPH.keySet()) {
            String searchName = slug.replace("-", " ");
            if (normalized.contains(searchName) || searchName.contains(normalized)) {
                return slug;
            }
        }
        return null;
    }

    @Transactional
    public int propagateBoost(UUID userId, String conceptSlug, int delta) {
        if (conceptSlug == null || delta == 0) {
            return 0;
        }
        List<Topic> all = topicRepository.findByUserId(userId, PageRequest.of(0, 200));
        List<String> related = getPrerequisites(conceptSlug);
        related.addAll(getDependents(conceptSlug));

        List<Topic> targets = all.stream()
                .filter(t -> {
                    String c = matchConcept(t.getTitle());
                    return c != null && related.contains(c);
                })
                .toList();

        for (Topic topic : targets) {
            int boost = (int) Math.round(delta * 0.35);
            int mastery = topic.getMastery() != null ? topic.getMastery() : 0;
            topic.setMastery(Math.max(0, Math.min(100, mastery + boost)));
            int confidence = topic.getConfidence() != null ? topic.getConfidence() : 0;
            topic.setConfidence(Math.max(0, Math.min(10, confidence + (boost > 0 ? 1 : -1))));
        }
        if (!targets.isEmpty()) {
            topicRepository.saveAll(targets);
        }
        return targets.size();
    }
}
