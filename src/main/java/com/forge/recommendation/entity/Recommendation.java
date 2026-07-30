package com.forge.recommendation.entity;

import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recommendations")
public class Recommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "INTEGER DEFAULT 1")
    private Integer priority = 1;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean dismissed = false;

    @Column(name = "problem_slug", length = 255)
    private String problemSlug;

    @Column(name = "problem_title", length = 255)
    private String problemTitle;

    @Column(name = "problem_difficulty", length = 50)
    private String problemDifficulty;

    public Recommendation withProblem(String title, String slug, String difficulty) {
        this.problemTitle = title;
        this.problemSlug = slug;
        this.problemDifficulty = difficulty;
        return this;
    }
}
