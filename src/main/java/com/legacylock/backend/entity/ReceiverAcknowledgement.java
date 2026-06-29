package com.legacylock.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "receiver_acknowledgements",
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_acknowledgement_access_grant",
                columnNames = "access_grant_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiverAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "access_grant_id", nullable = false)
    private AccessGrant accessGrant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private Users receiverUser;

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime acknowledgedAt;

    @PrePersist
    public void beforeCreate() {
        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = LocalDateTime.now();
        }
    }
}
