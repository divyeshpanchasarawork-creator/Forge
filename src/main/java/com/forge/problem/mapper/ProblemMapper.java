package com.forge.problem.mapper;

import com.forge.problem.dto.ProblemResponse;
import com.forge.problem.entity.Problem;
import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProblemMapper {

    public ProblemResponse toResponse(Problem problem) {
        List<ProblemResponse.TopicInfo> topicInfos = problem.getTopics().stream()
                .map(t -> new ProblemResponse.TopicInfo(t.getId(), t.getTitle(), t.getCategory()))
                .collect(Collectors.toList());

        return new ProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getLeetcodeId(),
                problem.getDifficulty(),
                problem.getTimeTaken(),
                problem.getAttempts(),
                problem.getConfidence(),
                problem.getMistakes(),
                problem.getSummary(),
                problem.getNotes(),
                problem.getSolutionUrl(),
                problem.getSolvedAt(),
                topicInfos,
                problem.getCreatedAt()
        );
    }
}
