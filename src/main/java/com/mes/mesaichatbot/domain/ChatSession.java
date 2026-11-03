package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {
    @Id
    @Column(name = "session_id")
    private String sessionId;

    @JoinColumn(name = "company_id", nullable = false)
    @ManyToOne
    private Company companyId;

    // UserMember가 복합키(company_id, employee_id)를 사용하므로 2개 컬럼을 모두 매핑
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", nullable = false)
    })
    private UserMember employeeId;

    @Column(name = "session_title", nullable = false)
    private String sessionTitle;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
