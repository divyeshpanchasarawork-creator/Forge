package com.forge.leetcode.service;

import com.forge.auth.entity.User;
import com.forge.leetcode.dto.LeetCodeGraphQlResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeetCodeTopicMapper {

    private final TopicRepository topicRepository;

    private static final Map<String, String> TAG_CATEGORIES = Map.ofEntries(
            Map.entry("Array", "Data Structures"),
            Map.entry("String", "Data Structures"),
            Map.entry("Hash Table", "Data Structures"),
            Map.entry("Linked List", "Data Structures"),
            Map.entry("Stack", "Data Structures"),
            Map.entry("Queue", "Data Structures"),
            Map.entry("Tree", "Data Structures"),
            Map.entry("Binary Tree", "Data Structures"),
            Map.entry("Heap (Priority Queue)", "Data Structures"),
            Map.entry("Graph", "Data Structures"),
            Map.entry("Trie", "Data Structures"),
            Map.entry("Union Find", "Data Structures"),
            Map.entry("Matrix", "Data Structures"),
            Map.entry("Binary Search Tree", "Data Structures"),
            Map.entry("Doubly-Linked List", "Data Structures"),
            Map.entry("Monotonic Stack", "Data Structures"),
            Map.entry("Segment Tree", "Data Structures"),
            Map.entry("Ordered Set", "Data Structures"),
            Map.entry("Rolling Hash", "Data Structures"),
            Map.entry("Hash Function", "Data Structures"),

            Map.entry("Two Pointers", "Algorithms"),
            Map.entry("Sliding Window", "Algorithms"),
            Map.entry("Binary Search", "Algorithms"),
            Map.entry("Sorting", "Algorithms"),
            Map.entry("Greedy", "Algorithms"),
            Map.entry("Divide and Conquer", "Algorithms"),
            Map.entry("Backtracking", "Algorithms"),
            Map.entry("Bit Manipulation", "Algorithms"),
            Map.entry("Recursion", "Algorithms"),
            Map.entry("Merge Sort", "Algorithms"),
            Map.entry("Counting Sort", "Algorithms"),
            Map.entry("Radix Sort", "Algorithms"),
            Map.entry("Bucket Sort", "Algorithms"),
            Map.entry("Quickselect", "Algorithms"),

            Map.entry("Depth-First Search", "Graph Algorithms"),
            Map.entry("Breadth-First Search", "Graph Algorithms"),
            Map.entry("Topological Sort", "Graph Algorithms"),
            Map.entry("Shortest Path", "Graph Algorithms"),
            Map.entry("Minimum Spanning Tree", "Graph Algorithms"),

            Map.entry("Dynamic Programming", "Dynamic Programming"),
            Map.entry("Memoization", "Dynamic Programming"),
            Map.entry("Knapsack", "Dynamic Programming"),

            Map.entry("Math", "Math & Number Theory"),
            Map.entry("Number Theory", "Math & Number Theory"),
            Map.entry("Combinatorics", "Math & Number Theory"),
            Map.entry("Probability and Statistics", "Math & Number Theory"),
            Map.entry("Game Theory", "Math & Number Theory"),

            Map.entry("Counting", "Frequency Counting"),

            Map.entry("Design", "System Design"),
            Map.entry("Data Stream", "System Design"),
            Map.entry("Iterator", "System Design"),
            Map.entry("Database", "SQL"),
            Map.entry("Shell", "Shell Scripting")
    );

    public List<Topic> mapToTopics(User user, List<LeetCodeGraphQlResponse.TagCount> tags, String skillLevel) {
        List<Topic> existingTopics = topicRepository.findByUserIdAndSource(user.getId(), "LEETCODE");

        if (tags == null || tags.isEmpty()) {
            if (!existingTopics.isEmpty()) {
                throw new IllegalStateException(
                        "LeetCode sync returned no tags while previously synced topics are present; aborting to protect existing data");
            }
            return new ArrayList<>();
        }

        Map<String, Topic> existingByTitle = new HashMap<>();
        for (Topic t : existingTopics) {
            existingByTitle.put(t.getTitle(), t);
        }

        List<Topic> result = new ArrayList<>();

        for (LeetCodeGraphQlResponse.TagCount tag : tags) {
            if (tag.getProblemsSolved() <= 0) continue;

            String title = tag.getTagName();
            Topic topic = existingByTitle.remove(title);

            if (topic == null) {
                topic = new Topic();
                topic.setUser(user);
                topic.setTitle(title);
                topic.setSource("LEETCODE");
                topic.setNotes("Auto-synced from LeetCode (" + skillLevel + " tier)");
            }

            topic.setCategory(TAG_CATEGORIES.getOrDefault(title, "Other"));
            topic.setConfidence(calculateConfidence(tag.getProblemsSolved()));
            topic.setMastery(calculateMastery(tag.getProblemsSolved()));
            topic.setStatus(calculateStatus(tag.getProblemsSolved()));

            if (topic.getDescription() == null || topic.getDescription().isBlank()) {
                topic.setDescription("LeetCode tag: " + tag.getTagSlug() + " | Solved: " + tag.getProblemsSolved() + " problems");
            }

            result.add(topic);
        }

        if (!existingByTitle.isEmpty()) {
            topicRepository.deleteAll(existingByTitle.values());
            log.info("Removed {} stale LeetCode-synced topics for user {}", existingByTitle.size(), user.getId());
        }

        return result;
    }

    private int calculateConfidence(int solved) {
        if (solved >= 50) return 9;
        if (solved >= 30) return 8;
        if (solved >= 20) return 7;
        if (solved >= 15) return 6;
        if (solved >= 10) return 5;
        if (solved >= 5) return 4;
        if (solved >= 3) return 3;
        return 2;
    }

    private int calculateMastery(int solved) {
        if (solved >= 50) return 90;
        if (solved >= 30) return 75;
        if (solved >= 20) return 60;
        if (solved >= 15) return 50;
        if (solved >= 10) return 40;
        if (solved >= 5) return 30;
        if (solved >= 3) return 20;
        return 10;
    }

    private String calculateStatus(int solved) {
        if (solved >= 50) return "MASTERED";
        return "IN_PROGRESS";
    }
}
