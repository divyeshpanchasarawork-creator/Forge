package com.forge.revision.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import com.forge.topic.entity.Topic;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "revisions")
public class Revision extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean completed = false;

    @Column(columnDefinition = "INTEGER DEFAULT 1")
    private Integer priority = 1;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    /**
     * Non-null only while the revision is pending (completed = false); cleared on completion.
     * Combined with the unique index on (user_id, pending_topic) this enforces at-most-one
     * pending revision per topic — NULLs never collide, so completed rows don't block the next
     * scheduled revision for the same topic.
     */
    @Column(name = "pending_topic")
    private UUID pendingTopic;
}
