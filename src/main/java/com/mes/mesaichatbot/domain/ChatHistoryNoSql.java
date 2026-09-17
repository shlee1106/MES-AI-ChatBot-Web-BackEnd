package com.mes.mesaichatbot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_histories")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatHistoryNoSql {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String chatId;
    private int companyId;
    private int employeeId;
    private String chatroomId;
    private String message;
    private LocalDateTime recordedAt;
    private String role;
}
