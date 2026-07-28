package com.forge.revision.mapper;

import com.forge.revision.dto.RevisionResponse;
import com.forge.revision.entity.Revision;
import org.springframework.stereotype.Component;

@Component
public class RevisionMapper {

    public RevisionResponse toResponse(Revision revision) {
        return new RevisionResponse(
                revision.getId(),
                revision.getTopic().getId(),
                revision.getTopic().getTitle(),
                revision.getTopic().getCategory(),
                revision.getScheduledDate(),
                revision.getCompleted(),
                revision.getPriority(),
                revision.getReason(),
                revision.getCompletionDate(),
                revision.getCreatedAt()
        );
    }
}
