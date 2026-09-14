package com.mes.mesaichatbot.domain;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@RedisHash("chat_histories_nosql")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatHistoryNoSql {
    @Id
    private int companyId;
    private int employeeId;
    private String chatroomId;
    private String message;
    private LocalDateTime recordedAt;
    private String role;
}
