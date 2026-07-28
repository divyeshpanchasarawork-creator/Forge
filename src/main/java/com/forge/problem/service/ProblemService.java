package com.forge.problem.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.dto.PagedResponse;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.problem.dto.ProblemRequest;
import com.forge.problem.dto.ProblemResponse;
import com.forge.problem.entity.Problem;
import com.forge.problem.mapper.ProblemMapper;
import com.forge.problem.repository.ProblemRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ProblemMapper problemMapper;

    public PagedResponse<ProblemResponse> getProblems(int page, int size, String difficulty, UUID topicId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("solvedAt").descending());

        Page<Problem> problemPage;
        if (difficulty != null && !difficulty.isBlank()) {
            problemPage = problemRepository.findByUserIdAndDifficulty(userId, difficulty, pageRequest);
        } else if (topicId != null) {
            problemPage = problemRepository.findByUserIdAndTopicId(userId, topicId, pageRequest);
        } else {
            problemPage = problemRepository.findByUserId(userId, pageRequest);
        }

        List<ProblemResponse> content = problemPage.getContent().stream()
                .map(problemMapper::toResponse)
                .toList();

        return new PagedResponse<>(content, page, size, problemPage.getTotalElements(), problemPage.getTotalPages(), problemPage.isLast());
    }

    public ProblemResponse createProblem(ProblemRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setTitle(request.getTitle());
        problem.setLeetcodeId(request.getLeetcodeId());
        problem.setDifficulty(request.getDifficulty());
        problem.setTimeTaken(request.getTimeTaken());
        problem.setAttempts(request.getAttempts() != null ? request.getAttempts() : 1);
        problem.setConfidence(request.getConfidence() != null ? request.getConfidence() : 0);
        problem.setMistakes(request.getMistakes());
        problem.setSummary(request.getSummary());
        problem.setNotes(request.getNotes());
        problem.setSolutionUrl(request.getSolutionUrl());
        problem.setSolvedAt(request.getSolvedAt() != null ? request.getSolvedAt() : java.time.LocalDateTime.now());

        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            Set<Topic> topics = new HashSet<>(topicRepository.findAllById(request.getTopicIds()));
            problem.setTopics(topics);
        }

        problem = problemRepository.save(problem);
        log.info("Problem created: {} for user: {}", problem.getTitle(), userId);
        return problemMapper.toResponse(problem);
    }

    public ProblemResponse updateProblem(UUID id, ProblemRequest request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", id));

        problem.setTitle(request.getTitle());
        problem.setLeetcodeId(request.getLeetcodeId());
        problem.setDifficulty(request.getDifficulty());
        problem.setTimeTaken(request.getTimeTaken());
        if (request.getAttempts() != null) problem.setAttempts(request.getAttempts());
        if (request.getConfidence() != null) problem.setConfidence(request.getConfidence());
        problem.setMistakes(request.getMistakes());
        problem.setSummary(request.getSummary());
        problem.setNotes(request.getNotes());
        problem.setSolutionUrl(request.getSolutionUrl());

        if (request.getTopicIds() != null) {
            Set<Topic> topics = new HashSet<>(topicRepository.findAllById(request.getTopicIds()));
            problem.setTopics(topics);
        }

        problem = problemRepository.save(problem);
        log.info("Problem updated: {}", problem.getTitle());
        return problemMapper.toResponse(problem);
    }

    public void deleteProblem(UUID id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", id));
        problemRepository.delete(problem);
        log.info("Problem deleted: {}", problem.getTitle());
    }
}
