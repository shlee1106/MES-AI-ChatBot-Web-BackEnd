package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_histories")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatHistory implements Persistable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String chatId;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company companyId;

    // UserMember가 복합키(company_id, employee_id)를 사용하므로 2개 컬럼을 모두 매핑
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", nullable = false)
    })
    private UserMember employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession sessionId;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private String role;

    @Override
    public @Nullable String getId() {
        return this.chatId;
    }

    @Override
    public boolean isNew() {
        return this.chatId == null;
    }
}
