package com.mes.mesaichatbot.domain;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@RedisHash("chat_histories_nosql")
@Getter
@Setter
public class ChatHistoryNoSql {
    @Id
    private String id;
    private String chatroomId;
    private String message;
    private LocalDateTime recordedAt;
    private String role;
}
