package com.forge.topic.service;

import com.forge.common.util.SecurityUtils;
import com.forge.topic.dto.TopicResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.mapper.TopicMapper;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    @Transactional(readOnly = true)
    public List<TopicResponse> getWeakTopics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findWeakTopicsByUserId(userId).stream()
                .map(topicMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getStrongTopics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findStrongTopicsByUserId(userId).stream()
                .map(topicMapper::toResponse)
                .toList();
    }
}
