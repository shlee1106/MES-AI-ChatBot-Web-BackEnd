package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.config.MockMvcTestConfig;
import com.mes.mesaichatbot.domain.jwt.service.RefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("JWTController")
@WebMvcTest(JWTController.class)
@WithMockUser(username = "admin", roles = "MASTER")
@Import(MockMvcTestConfig.class)
class JWTControllerTest {
    @MockitoBean
    RefreshService service;
    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("토큰 갱신 시 결과 혹은 오류를 반환하는가?")
    void refreshReturnsBearerTokenAndRejectsMissingOrInvalidCookie() throws Exception {
        when(service.refresh("refresh")).thenReturn("access");
        var result = mvc.perform(post("/api/v1/jwt/refresh").with(csrf()).cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh"))).andExpect(status().isOk());
        result.andExpect(header().string("Authorization", "Bearer access")).andExpect(content().string("토큰 재발급 성공"));
        when(service.refresh("")).thenThrow(new IllegalArgumentException("missing"));
        mvc.perform(post("/api/v1/jwt/refresh").with(csrf())).andExpect(status().isUnauthorized()).andExpect(content().string("Refresh Token이 존재하지 않습니다."));
        when(service.refresh("bad")).thenThrow(new IllegalArgumentException("expired"));
        mvc.perform(post("/api/v1/jwt/refresh").with(csrf()).cookie(new jakarta.servlet.http.Cookie("refreshToken", "bad"))).andExpect(status().isUnauthorized()).andExpect(content().string(containsString("expired")));
    }
}
