package com.legacylock.backend.entity;

import com.legacylock.backend.enums.ReleasePolicyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "release_policies",
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_release_policy_capsule",
                columnNames = "capsule_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleasePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @Column(nullable = false)
    private Integer inactivityDays;

    @Column(nullable = false)
    private Integer graceDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReleasePolicyStatus status;

    @Column(name = "first_reminder_sent_at")
    private LocalDateTime firstReminderSentAt;

    @Column(name = "second_reminder_sent_at")
    private LocalDateTime secondReminderSentAt;

    @Column(name = "final_reminder_sent_at")
    private LocalDateTime finalReminderSentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void beforeCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = ReleasePolicyStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
