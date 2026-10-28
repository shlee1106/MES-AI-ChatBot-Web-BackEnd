package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import com.mes.mesaichatbot.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    UserService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();
    }

    @Test
    @DisplayName("내 정보와 관리자 사용자 목록 조회 및 미인증 시 오류를 반환하는가?")
    void readsCurrentAndAdminUsersAndMapsUnauthenticated() throws Exception {
        var user = new UserInfoResponseDTO(1L, 2L, "Kim", "k@example.com", UserRole.ROLE_USER, "MES", null);
        when(service.getMyInfo()).thenReturn(user);
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("k@example.com"));
        when(service.getUserForAdmin()).thenReturn(List.of(user));
        mvc.perform(get("/api/v1/users/")).andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("Kim"));
        when(service.getMyInfo()).thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
        when(service.getUserForAdmin()).thenThrow(new NullPointerException());
        mvc.perform(get("/api/v1/users/")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("사용자 삭제 시 결과 혹은 오류를 반환하는가?")
    void deletesUsersWithSuccessAndErrorStatuses() throws Exception {
        assertThat(mvc.perform(delete("/api/v1/users/3")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isNotEmpty();
        doThrow(new AccessDeniedException("forbidden")).when(service).deleteUser(3L);
        mvc.perform(delete("/api/v1/users/3")).andExpect(status().isForbidden()).andExpect(content().string("forbidden"));
        doThrow(new UsernameNotFoundException("missing")).when(service).deleteUser(4L);
        mvc.perform(delete("/api/v1/users/4")).andExpect(status().isNotFound()).andExpect(content().string(""));
        doThrow(new NullPointerException()).when(service).deleteUser(5L);
        assertThat(mvc.perform(delete("/api/v1/users/5")).andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString()).isNotEmpty();
    }

    @Test
    @DisplayName("권한 변경 시 결과 혹은 오류를 반환하는가?")
    void updatesRolesWithSuccessAndErrorStatuses() throws Exception {
        assertThat(mvc.perform(patch("/api/v1/users/3").contentType("text/plain").content("ADMIN")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isNotEmpty();
        doThrow(new AccessDeniedException("forbidden")).when(service).updateRole(3L, "ADMIN");
        mvc.perform(patch("/api/v1/users/3").contentType("text/plain").content("ADMIN")).andExpect(status().isForbidden()).andExpect(content().string("forbidden"));
        doThrow(new UsernameNotFoundException("missing")).when(service).updateRole(4L, "ADMIN");
        mvc.perform(patch("/api/v1/users/4").contentType("text/plain").content("ADMIN")).andExpect(status().isNotFound()).andExpect(content().string(""));
        doThrow(new IllegalArgumentException("invalid")).when(service).updateRole(5L, "BAD");
        mvc.perform(patch("/api/v1/users/5").contentType("text/plain").content("BAD")).andExpect(status().isBadRequest()).andExpect(content().string("invalid"));
        doThrow(new NullPointerException()).when(service).updateRole(6L, "ADMIN");
        assertThat(mvc.perform(patch("/api/v1/users/6").contentType("text/plain").content("ADMIN")).andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString()).isNotEmpty();
    }
}
