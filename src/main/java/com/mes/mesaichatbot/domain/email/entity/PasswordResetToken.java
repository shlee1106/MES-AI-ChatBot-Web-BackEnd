package com.mes.mesaichatbot.domain.email.entity;


import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

@Getter
@AllArgsConstructor
@RedisHash(value = "resetToken", timeToLive = 300)
public class PasswordResetToken {

    @Id
    private String id;

    private Long companyId;
    private Long employeeId;
}
