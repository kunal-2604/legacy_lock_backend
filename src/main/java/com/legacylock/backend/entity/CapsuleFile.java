package com.legacylock.backend.entity;

import com.legacylock.backend.enums.CapsuleFileStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "capsule_files",
        indexes = {
                @Index(name = "idx_capsule_file_capsule_id", columnList = "capsule_id"),
                @Index(name = "idx_capsule_file_stored_key", columnList = "stored_file_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_key", nullable = false, unique = true, length = 500)
    private String storedFileKey;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CapsuleFileStatus status;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void beforeCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = CapsuleFileStatus.ACTIVE;
        }
    }
}
