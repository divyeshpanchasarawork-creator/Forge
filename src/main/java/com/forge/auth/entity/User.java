package com.forge.auth.entity;

import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column
    private String password;

    @Column(length = 100)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "leetcode_username", length = 50)
    private String leetcodeUsername;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "target_level")
    private Integer targetLevel = 5;

    @Column(name = "preferred_analysis_time")
    private java.time.LocalTime preferredAnalysisTime;

    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";

    @Column(name = "daily_generations_used")
    private Integer dailyGenerationsUsed = 0;

    @Column(name = "last_generation_date")
    private java.time.LocalDate lastGenerationDate;
}
