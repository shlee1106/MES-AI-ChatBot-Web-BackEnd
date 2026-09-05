package com.example.seven.domain.jwt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;


@Getter
@Setter
@RedisHash(value = "refreshToken", timeToLive = 2592000)
public class RefreshEntity {


    private String username;

    @Id
    private String refresh;
}
