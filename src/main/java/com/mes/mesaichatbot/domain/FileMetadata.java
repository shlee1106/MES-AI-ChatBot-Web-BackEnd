package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * file_metadata 테이블.
 */
@Entity
@Table(name = "file_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FileMetadata {
    @Id
    @Column(name = "file_id")
    private String fileId;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company companyId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private UserMember employeeId;

    @Column(name = "origin_name", nullable = false)
    private String originName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
