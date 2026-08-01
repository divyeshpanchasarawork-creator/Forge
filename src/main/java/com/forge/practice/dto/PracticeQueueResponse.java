package com.forge.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQueueResponse {

    private String profile;
    private String planMessage;
    private List<PracticeProblemResponse> queue;
    private List<String> revisitTopics;
}
