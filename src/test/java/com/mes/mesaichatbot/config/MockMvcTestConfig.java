package com.mes.mesaichatbot.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

/**
 * MockMvc의 기본 설정을 구성하는 테스트용 구성 클래스입니다.
 */
@TestConfiguration
public class MockMvcTestConfig {
    /**
     * 자동 주입되는 MockMvc의 기본 응답 문자 인코딩을 UTF-8로 설정합니다.
     */
    @Bean
    public MockMvcBuilderCustomizer mockMvcBuilderCustomizer() {
        return builder -> builder.defaultResponseCharacterEncoding(StandardCharsets.UTF_8);
    }
}
