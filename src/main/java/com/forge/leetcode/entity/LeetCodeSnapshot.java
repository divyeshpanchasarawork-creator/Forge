package com.forge.leetcode.entity;

import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leetcode_snapshots")
public class LeetCodeSnapshot extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_solved")
    private Integer totalSolved = 0;

    @Column(name = "easy_solved")
    private Integer easySolved = 0;

    @Column(name = "medium_solved")
    private Integer mediumSolved = 0;

    @Column(name = "hard_solved")
    private Integer hardSolved = 0;

    @Column(name = "easy_beats_pct")
    private Double easyBeatsPct;

    @Column(name = "medium_beats_pct")
    private Double mediumBeatsPct;

    @Column(name = "hard_beats_pct")
    private Double hardBeatsPct;

    private Integer ranking;

    @Column(name = "contest_rating")
    private Double contestRating;

    @Column(name = "contest_ranking")
    private Integer contestRanking;

    @Column(name = "contest_attended_count")
    private Integer contestAttendedCount = 0;

    private Integer streak = 0;

    @Column(name = "total_active_days")
    private Integer totalActiveDays = 0;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
