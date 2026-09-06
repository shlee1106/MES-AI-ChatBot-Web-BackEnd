package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "column_codes")
@Getter
@Setter
public class ColumnCode {
    @Id
    @Column(name = "column_code_id")
    private Long columnCodeId;

    @JoinColumn(name = "company_id", nullable = false)
    @ManyToOne
    private Company companyId;

    @Column(name = "excel_column_name", nullable = false, length = 100)
    private String excelColumnName;

    @Column(name = "standard_column_code", nullable = false, length = 50)
    private String standardColumnCode;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;
}
