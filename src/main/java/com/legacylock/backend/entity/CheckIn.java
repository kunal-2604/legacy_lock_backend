package com.legacylock.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime checkedInAt;

    @PrePersist
    public void beforeCreate() {
        if (this.checkedInAt == null) {
            this.checkedInAt = LocalDateTime.now();
        }
    }
}
