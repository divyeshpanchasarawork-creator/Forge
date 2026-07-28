package com.forge.problem.entity;

import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import com.forge.topic.entity.Topic;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "problems")
public class Problem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "leetcode_id", length = 20)
    private String leetcodeId;

    @Column(nullable = false, length = 10)
    private String difficulty;

    @Column(name = "time_taken")
    private Integer timeTaken;

    @Column(columnDefinition = "INTEGER DEFAULT 1")
    private Integer attempts = 1;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer confidence = 0;

    @Column(columnDefinition = "TEXT")
    private String mistakes;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "solution_url", length = 500)
    private String solutionUrl;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "problem_topics",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();
}
