package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "original_file")
@Getter
@Setter
public class OriginalFile {
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

    @Column(name = "save_file_name", nullable = false)
    private String saveFileName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_data_path", nullable = false)
    private String fileDataPath;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
