package com.forge.revision.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevisionResponse {

    private UUID id;
    private UUID topicId;
    private String topicTitle;
    private String topicCategory;
    private LocalDate scheduledDate;
    private Boolean completed;
    private Integer priority;
    private String reason;
    private LocalDateTime completionDate;
    private LocalDateTime createdAt;
}
