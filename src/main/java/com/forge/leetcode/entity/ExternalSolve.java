package com.forge.leetcode.entity;

import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "external_solves",
        uniqueConstraints = @UniqueConstraint(name = "uq_external_solves_user_slug", columnNames = {"user_id", "title_slug"}))
public class ExternalSolve extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;

    @Column(name = "title_slug", nullable = false)
    private String titleSlug;

    private LocalDateTime solvedAt;

    @Column(nullable = false)
    private boolean logged = false;
}
