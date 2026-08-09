package com.forge.topic.dto;

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
public class TopicResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;
    private Integer confidence;
    private Integer mastery;
    private String notes;
    private LocalDateTime lastRevision;
    private LocalDate nextRevision;
    private String status;
    private Integer revisionCount;
    private Double estimatedRetention;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
