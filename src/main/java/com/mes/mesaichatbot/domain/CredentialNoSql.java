package com.mes.mesaichatbot.domain;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@RedisHash("credentials")
@Getter
@Setter
public class CredentialNoSql {
    @Id
    private String _id;

    private String passwordHash;
    private int passwordFailedCount;
    private LocalDateTime recentChangedAt;
}
