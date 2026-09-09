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

    @JoinColumn(name = "employee_id", nullable = false)
    @ManyToOne
    private UserMember employeeId;

    @Column(name = "session_title", nullable = false)
    private String sessionTitle;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
