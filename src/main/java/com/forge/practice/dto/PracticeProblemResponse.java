package com.forge.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProblemResponse {

    private String title;
    private String titleSlug;
    private String difficulty;
    private String topicTag;
    private String reason;
}
