package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.config.MockMvcTestConfig;
import com.mes.mesaichatbot.dto.PasswordResetRequestDTO;
import com.mes.mesaichatbot.dto.PasswordUpdateRequestDTO;
import com.mes.mesaichatbot.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@WithMockUser(username = "admin", roles = "MASTER")
@Import(MockMvcTestConfig.class)
class EmailControllerTest {
    @MockitoBean
    PasswordResetService service;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;


    @Test
    @DisplayName("재설정 이메일 요청 시 성공 혹은 오류를 반환하는가?")
    void requestResetEmailReturnsSuccessAndValidationError() throws Exception {
        String body = mapper.writeValueAsString(
                new PasswordResetRequestDTO(1L, 2L, "person@example.com"));
        mvc.perform(post("/api/v1/password/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                                                    .content(body))
           .andExpect(status().isOk())
           .andExpect(content().string("비밀번호 재설정 이메일이 성공적으로 발송되었습니다."));

        doThrow(new IllegalArgumentException("invalid")).when(service).sendResetEmail(any());
        mvc.perform(post("/api/v1/password/request").with(csrf())
                                                    .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isBadRequest())
           .andExpect(content().string("invalid"));
    }

    @Test
    @DisplayName("토큰 검증 시 결과 혹은 오류를 반환하는가?")
    void verifiesTokenAndReturnsNotFoundWhenInvalid() throws Exception {
        mvc.perform(get("/api/v1/password/token/good"))
           .andExpect(status().isOk())
           .andExpect(content().string("true"));

        doThrow(new IllegalArgumentException()).when(service).verifyTokenExists("bad");
        mvc.perform(get("/api/v1/password/token/bad"))
           .andExpect(status().isNotFound())
           .andExpect(content().string(""));
    }

    @Test
    @DisplayName("비밀번호 변경 시 결과 혹은 오류를 반환하는가?")
    void updatesPasswordAndRejectsMissingFields() throws Exception {
        String valid = mapper.writeValueAsString(new PasswordUpdateRequestDTO("token", "secret1234!"));

        String invalidToken = mapper.writeValueAsString(new PasswordUpdateRequestDTO(null, "secret1234!"));
        String invalidPassword = mapper.writeValueAsString(new PasswordUpdateRequestDTO("x", " "));
        mvc.perform(post("/api/v1/password/update")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(valid))
           .andExpect(status().isOk())
           .andExpect(content().string("비밀번호가 성공적으로 변경되었습니다."));
        mvc.perform(post("/api/v1/password/update")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalidToken))
           .andExpect(status().isBadRequest())
           .andExpect(content().string("토큰은 필수입니다."));
        mvc.perform(post("/api/v1/password/update")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalidPassword))
           .andExpect(status().isBadRequest())
           .andExpect(content().string(containsString("새 비밀번호는 필수입니다.")));
        doThrow(new IllegalArgumentException("expired"))
                .when(service).updatePassword("bad", "secret1234!");
        mvc.perform(post("/api/v1/password/update")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(
                           mapper.writeValueAsString(
                                   new PasswordUpdateRequestDTO("bad", "secret1234!"))))
           .andExpect(status().isBadRequest())
           .andExpect(content().string("expired"));
    }
}
