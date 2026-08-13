package com.forge.leetcode.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "leetcode_tag_stats")
public class LeetCodeTagStat extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "tag_slug", nullable = false, length = 100)
    private String tagSlug;

    @Column(name = "problems_solved")
    private Integer problemsSolved = 0;

    @Column(name = "skill_level", length = 20)
    private String skillLevel;
}
