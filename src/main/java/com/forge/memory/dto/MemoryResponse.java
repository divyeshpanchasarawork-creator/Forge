package com.forge.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoryResponse {

    private List<FadingConcept> fadingConcepts;
    private List<MemoryEntry> patternsDiscovered;
    private List<MemoryEntry> pastMistakes;
    private List<MemoryEntry> insights;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FadingConcept {
        private String topicId;
        private String title;
        private String category;
        private int confidence;
        private int mastery;
        private long daysSinceRevision;
        private Double estimatedRetention;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryEntry {
        private LocalDate date;
        private String content;
        private String topicTitle;
        private String topicCategory;
    }
}
