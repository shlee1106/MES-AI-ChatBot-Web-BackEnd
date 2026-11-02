package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.config.MockMvcTestConfig;
import com.mes.mesaichatbot.domain.user.dto.JoinRequestDTO;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import com.mes.mesaichatbot.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("JoinController")
@WebMvcTest(JoinController.class)
@WithMockUser(username = "admin", roles = "MASTER")
@Import(MockMvcTestConfig.class)
class JoinControllerTest {
    @MockitoBean
    private UserService service;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("사용자 생성 시 성공 혹은 오류를 반환하는가?")
    void createsUserAndMapsInvalidRequest() throws Exception {
        var dto = new JoinRequestDTO(1L, 2L, "Password1!", "홍길동", "user@example.com", UserRole.ROLE_USER);
        mvc.perform(post("/api/v1/admin/users").with(csrf()).contentType("application/json").content(mapper.writeValueAsString(dto))).andExpect(status().isCreated()).andExpect(content().string("계정이 성공적으로 생성 완료."));
        doThrow(new IllegalArgumentException("duplicate")).when(service).createUser(any());
        mvc.perform(post("/api/v1/admin/users").with(csrf()).contentType("application/json").content(mapper.writeValueAsString(dto))).andExpect(status().isBadRequest()).andExpect(content().string("duplicate"));
    }

    @Test
    @DisplayName("필수 입력값이 누락되면 오류를 반환하는가?")
    void validatesRequiredFields() throws Exception {
        mvc.perform(post("/api/v1/admin/users").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest())
                .andExpect(content().string(
                        allOf(
                                containsString("비밀번호는 필수 입력값입니다."),
                                containsString("권한은 필수 입력값입니다."),
                                containsString("회사 번호는 필수 입력값입니다."),
                                containsString("이름은 필수 입력값입니다."),
                                containsString("이메일은 필수 입력값입니다."),
                                containsString("사원 번호는 필수 입력값입니다."))));
        verifyNoInteractions(service);
    }
}
