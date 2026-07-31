package com.forge.leetcode.entity;

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
@Table(name = "problem_suggestions")
public class ProblemSuggestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "title_slug", nullable = false, length = 255)
    private String titleSlug;

    @Column(nullable = false, length = 50)
    private String difficulty;

    @Column(name = "topic_tag_slug", length = 100)
    private String topicTagSlug;

    @Column(name = "topic_tag_name", length = 100)
    private String topicTagName;

    @Column(nullable = false, length = 20)
    private String source = "WEAK_TAG";
}
