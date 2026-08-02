package com.forge.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequest {

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must be valid")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    @Size(max = 50, message = "LeetCode username must be at most 50 characters")
    private String leetcodeUsername;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;

    @Min(value = 1, message = "Target level must be between 1 and 10")
    @Max(value = 10, message = "Target level must be between 1 and 10")
    private Integer targetLevel;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Analysis time must be in HH:mm format")
    private String preferredAnalysisTime;
}
