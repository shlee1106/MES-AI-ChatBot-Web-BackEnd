package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.email.dto.PasswordResetRequestDTO;
import com.mes.mesaichatbot.domain.email.dto.PasswordUpdateRequestDTO;
import com.mes.mesaichatbot.domain.email.serivce.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("EmailController")
@ExtendWith(MockitoExtension.class)
class EmailControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock
    PasswordResetService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EmailController(service)).addFilters(new CharacterEncodingFilter("UTF-8", true)).build();
    }

    @Test
    @DisplayName("재설정 이메일 요청 시 성공 혹은 오류를 반환하는가?")
    void requestResetEmailReturnsSuccessAndValidationError() throws Exception {
        String body = mapper.writeValueAsString(new PasswordResetRequestDTO(1L, 2L, "person@example.com"));
        mvc.perform(post("/api/v1/password/request").contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(content().string("비밀번호 재설정 이메일이 성공적으로 발송되었습니다."));
        doThrow(new IllegalArgumentException("invalid")).when(service).sendResetEmail(any());
        mvc.perform(post("/api/v1/password/request").contentType("application/json").content(body)).andExpect(status().isBadRequest()).andExpect(content().string("invalid"));
    }

    @Test
    @DisplayName("토큰 검증 시 결과 혹은 오류를 반환하는가?")
    void verifiesTokenAndReturnsNotFoundWhenInvalid() throws Exception {
        assertThat(mvc.perform(get("/api/v1/password/token/good")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isEqualTo("true");
        doThrow(new IllegalArgumentException()).when(service).verifyTokenExists("bad");
        mvc.perform(get("/api/v1/password/token/bad")).andExpect(status().isNotFound()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("비밀번호 변경 시 결과 혹은 오류를 반환하는가?")
    void updatesPasswordAndRejectsMissingFields() throws Exception {
        String valid = mapper.writeValueAsString(new PasswordUpdateRequestDTO("token", "secret"));
        assertThat(mvc.perform(post("/api/v1/password/update").contentType("application/json").content(valid)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isNotEmpty();
        assertThat(mvc.perform(post("/api/v1/password/update").contentType("application/json").content("{\"token\":null,\"newPassword\":\"abc\"}")).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString()).isNotEmpty();
        assertThat(mvc.perform(post("/api/v1/password/update").contentType("application/json").content("{\"token\":\"x\",\"newPassword\":\" \"}")).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString()).isNotEmpty();
        doThrow(new IllegalArgumentException("expired")).when(service).updatePassword("bad", "secret");
        assertThat(mvc.perform(post("/api/v1/password/update").contentType("application/json").content(mapper.writeValueAsString(new PasswordUpdateRequestDTO("bad", "secret")))).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString()).isEqualTo("expired");
    }
}
