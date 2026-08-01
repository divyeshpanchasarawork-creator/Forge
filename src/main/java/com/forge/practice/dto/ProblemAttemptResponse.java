package com.forge.practice.dto;

import com.forge.practice.entity.ProblemAttempt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemAttemptResponse {

    private ProblemAttempt attempt;
    private List<String> topicsUpdated;
    private String feedback;
}
