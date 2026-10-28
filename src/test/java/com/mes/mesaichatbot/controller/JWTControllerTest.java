package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.jwt.service.RefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("JWTController")
@ExtendWith(MockitoExtension.class)
class JWTControllerTest {
    @Mock
    RefreshService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new JWTController(service)).build();
    }

    @Test
    @DisplayName("토큰 갱신 시 결과 혹은 오류를 반환하는가?")
    void refreshReturnsBearerTokenAndRejectsMissingOrInvalidCookie() throws Exception {
        when(service.refresh("refresh")).thenReturn("access");
        var success = mvc.perform(post("/api/v1/jwt/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh"))).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(success.getHeader("Authorization")).isEqualTo("Bearer access");
        assertThat(success.getContentAsString()).isNotEmpty();
        assertThat(mvc.perform(post("/api/v1/jwt/refresh")).andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString()).isNotEmpty();
        when(service.refresh("bad")).thenThrow(new IllegalArgumentException("expired"));
        assertThat(mvc.perform(post("/api/v1/jwt/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", "bad"))).andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString()).contains("expired");
    }
}
