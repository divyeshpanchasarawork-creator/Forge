package com.forge.topic.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.dto.PagedResponse;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.topic.dto.TopicRequest;
import com.forge.topic.dto.TopicResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.mapper.TopicMapper;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TopicMapper topicMapper;

    public PagedResponse<TopicResponse> getTopics(int page, int size, String category, String status) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Topic> topicPage;
        if (category != null && !category.isBlank()) {
            topicPage = topicRepository.findByUserIdAndCategory(userId, category, pageRequest);
        } else if (status != null && !status.isBlank()) {
            topicPage = topicRepository.findByUserIdAndStatus(userId, status, pageRequest);
        } else {
            topicPage = topicRepository.findByUserId(userId, pageRequest);
        }

        List<TopicResponse> content = topicPage.getContent().stream()
                .map(topicMapper::toResponse)
                .toList();

        return new PagedResponse<>(content, page, size, topicPage.getTotalElements(), topicPage.getTotalPages(), topicPage.isLast());
    }

    public TopicResponse getTopicById(UUID id) {
        return topicMapper.toResponse(findOwnedTopic(id));
    }

    public TopicResponse createTopic(TopicRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Topic topic = new Topic();
        topic.setUser(user);
        topic.setTitle(request.getTitle());
        topic.setDescription(request.getDescription());
        topic.setCategory(request.getCategory());
        topic.setConfidence(request.getConfidence() != null ? request.getConfidence() : 0);
        topic.setMastery(request.getMastery() != null ? request.getMastery() : 0);
        topic.setNotes(request.getNotes());

        topic = topicRepository.save(topic);
        log.info("Topic created: {} for user: {}", topic.getTitle(), userId);
        return topicMapper.toResponse(topic);
    }

    public TopicResponse updateTopic(UUID id, TopicRequest request) {
        Topic topic = findOwnedTopic(id);

        if (request.getTitle() != null) topic.setTitle(request.getTitle());
        if (request.getDescription() != null) topic.setDescription(request.getDescription());
        if (request.getCategory() != null) topic.setCategory(request.getCategory());
        if (request.getConfidence() != null) topic.setConfidence(request.getConfidence());
        if (request.getMastery() != null) topic.setMastery(request.getMastery());
        if (request.getNotes() != null) topic.setNotes(request.getNotes());

        topic = topicRepository.save(topic);
        log.info("Topic updated: {}", topic.getTitle());
        return topicMapper.toResponse(topic);
    }

    public void deleteTopic(UUID id) {
        Topic topic = findOwnedTopic(id);
        topicRepository.delete(topic);
        log.info("Topic deleted: {}", topic.getTitle());
    }

    private Topic findOwnedTopic(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", id));
        if (!topic.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Topic", "id", id);
        }
        return topic;
    }

    public List<TopicResponse> getWeakTopics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findWeakTopicsByUserId(userId).stream()
                .map(topicMapper::toResponse)
                .toList();
    }

    public List<TopicResponse> getStrongTopics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findStrongTopicsByUserId(userId).stream()
                .map(topicMapper::toResponse)
                .toList();
    }

    public List<TopicResponse> getTopicsNeedingRevision() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findTopicsNeedingRevisionByUserId(userId).stream()
                .map(topicMapper::toResponse)
                .toList();
    }
}
