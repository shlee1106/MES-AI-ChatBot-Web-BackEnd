package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.dto.JoinRequestDTO;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import com.mes.mesaichatbot.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("JoinController")
@ExtendWith(MockitoExtension.class)
class JoinControllerTest {
    @Mock
    UserService service;
    MockMvc mvc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new JoinController(service)).build();
    }

    @Test
    @DisplayName("사용자 생성 시 성공 혹은 오류를 반환하는가?")
    void createsUserAndMapsInvalidRequest() throws Exception {
        var dto = new JoinRequestDTO(1L, 2L, "Password1!", "홍길동", "user@example.com", UserRole.ROLE_USER);
        var response = mvc.perform(post("/api/v1/admin/users").contentType("application/json").content(mapper.writeValueAsString(dto))).andExpect(status().isCreated()).andReturn().getResponse();
        assertThat(response.getContentAsString()).isNotEmpty();
        doThrow(new IllegalArgumentException("duplicate")).when(service).createUser(any());
        assertThat(mvc.perform(post("/api/v1/admin/users").contentType("application/json").content(mapper.writeValueAsString(dto))).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString()).isEqualTo("duplicate");
    }

    @Test
    @DisplayName("필수 입력값이 누락되면 오류를 반환하는가?")
    void validatesRequiredFields() throws Exception {
        mvc.perform(post("/api/v1/admin/users").contentType("application/json").content("{}")).andExpect(status().isBadRequest()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
        verifyNoInteractions(service);
    }
}
