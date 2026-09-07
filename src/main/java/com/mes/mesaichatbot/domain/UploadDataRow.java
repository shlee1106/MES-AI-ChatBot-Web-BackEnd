package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "upload_data_rows")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadDataRow {
    @Id
    @Column(name = "data_id")
    private Long dataId;
    @JoinColumn(name = "company_id", nullable = false)
    @ManyToOne
    private Company companyId;
    @JoinColumn(name = "file_id", nullable = false)
    @ManyToOne
    private OriginalFile fileId;
    @Column(name = "`row_number`", nullable = false)
    private int rowNumber;
    @Column(name = "row_data", nullable = false, columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rowData;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
