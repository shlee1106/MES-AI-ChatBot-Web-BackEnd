package com.mes.mesaichatbot.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;


@Getter
@Setter
@RedisHash(value = "refreshToken", timeToLive = 2592000)
public class RefreshEntity {


    @Indexed
    private String username;

    @Id
    private String refresh;
}
