package com.forge.topic.mapper;

import com.forge.topic.dto.TopicResponse;
import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Component;

@Component
public class TopicMapper {

    public TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getCategory(),
                topic.getConfidence(),
                topic.getMastery(),
                topic.getNotes(),
                topic.getLastRevision(),
                topic.getNextRevision(),
                topic.getStatus(),
                topic.getRevisionCount(),
                topic.getEstimatedRetention(),
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }
}
