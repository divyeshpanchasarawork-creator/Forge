package com.forge.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemoryResponse {

    private List<FadingConcept> fadingConcepts;

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
        private String suggestedProblemTitle;
        private String suggestedProblemSlug;
        private String suggestedProblemDifficulty;
    }
}
