package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // UserMember가 복합키(company_id, employee_id)를 사용하므로 2개 컬럼을 모두 매핑
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", nullable = false),
            @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", nullable = false)
    })
    private UserMember employeeId;

    @Column(name = "origin_name", nullable = false)
    private String originName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "shared", nullable = false)
    private boolean shared;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    public Company getCompanyId() {
        return employeeId.getCompany();
    }
}
