package com.legacylock.backend.entity;

import com.legacylock.backend.enums.AccessGrantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "access_grants",
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_access_grant_capsule_receiver",
                columnNames = {"capsule_id", "receiver_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Receiver receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccessGrantStatus status;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    @Column
    private LocalDateTime revokedAt;

    @PrePersist
    public void beforeCreate() {
        if (this.status == null) {
            this.status = AccessGrantStatus.ACTIVE;
        }

        if (this.grantedAt == null) {
            this.grantedAt = LocalDateTime.now();
        }
    }
}
