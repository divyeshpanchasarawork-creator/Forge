package com.forge.topic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be under 200 characters")
    private String title;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private Integer confidence;
    private Integer mastery;
    private String notes;
}
