package com.forge.journal.entity;

import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "journals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "entry_date"})
})
public class Journal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "morning_goal", columnDefinition = "TEXT")
    private String morningGoal;

    @Column(name = "evening_reflection", columnDefinition = "TEXT")
    private String eveningReflection;

    private Integer energy;

    private Integer mood;

    @Column(name = "hours_studied", columnDefinition = "DOUBLE DEFAULT 0")
    private Double hoursStudied = 0.0;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    @Column(columnDefinition = "TEXT")
    private String challenges;

    @Column(columnDefinition = "TEXT")
    private String lessons;
}
