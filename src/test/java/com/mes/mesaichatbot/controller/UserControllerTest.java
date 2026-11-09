package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.config.MockMvcTestConfig;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.dto.JoinRequestDTO;
import com.mes.mesaichatbot.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController")
@WebMvcTest(UserController.class)
@WithMockUser(username = "admin", roles = "MASTER")
@Import(MockMvcTestConfig.class)
class UserControllerTest {
    private final JoinRequestDTO joinRequest =
            new JoinRequestDTO(1L, 2L, "Password1!",
                               "홍길동", "user@example.com", UserRole.ROLE_USER);
    @MockitoBean
    UserService service;
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("내 정보와 관리자 사용자 목록 조회 및 미인증 시 오류를 반환하는가?")
    void readsCurrentAndAdminUsersAndMapsUnauthenticated() throws Exception {
        UserInfoResponseDTO user = new UserInfoResponseDTO(
                1L, 2L, "Kim", "k@example.com", UserRole.ROLE_USER,
                "MES", null);
        when(service.getMyInfo()).thenReturn(user);
        mvc.perform(get("/api/v1/users/me").with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value("k@example.com"));
        when(service.getUserForAdmin()).thenReturn(List.of(user));
        mvc.perform(get("/api/v1/users").with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].username").value("Kim"));
        when(service.getMyInfo()).thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(get("/api/v1/users/me").with(csrf()))
           .andExpect(status().isUnauthorized())
           .andExpect(content().string(""));
        when(service.getUserForAdmin()).thenThrow(new NullPointerException());
        mvc.perform(get("/api/v1/users").with(csrf()))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error").value("세션이 유효하지 않습니다."));
    }

    @Test
    @DisplayName("관리자 사용자 목록 조회 권한이 없으면 403을 반환하는가?")
    void mapsAccessDeniedWhenReadingAdminUsers() throws Exception {
        when(service.getUserForAdmin()).thenThrow(new AccessDeniedException("forbidden"));
        mvc.perform(get("/api/v1/users").with(csrf()))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @DisplayName("사용자 생성 시 성공하면 201을 반환하는가?")
    void createsUser() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf())
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .content(mapper.writeValueAsString(joinRequest)))
           .andExpect(status().isCreated())
           .andExpect(content().string("계정이 성공적으로 생성 완료."));
        verify(service).createUser(joinRequest);
    }

    @Test
    @DisplayName("사용자 생성 시 서비스 예외를 알맞은 상태 코드로 변환하는가?")
    void mapsCreateUserErrors() throws Exception {
        String body = mapper.writeValueAsString(joinRequest);

        doThrow(new AccessDeniedException("forbidden")).when(service).createUser(any());
        mvc.perform(post("/api/v1/users").with(csrf())
                                         .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error").value("forbidden"));

        doThrow(new DuplicateKeyException("duplicate")).when(service).createUser(any());
        mvc.perform(post("/api/v1/users").with(csrf())
                                         .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.error").value("duplicate"));

        doThrow(new IllegalArgumentException("invalid")).when(service).createUser(any());
        mvc.perform(post("/api/v1/users").with(csrf())
                                         .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").value("invalid"));

        doThrow(new NullPointerException()).when(service).createUser(any());
        mvc.perform(post("/api/v1/users").with(csrf())
                                         .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error").value("세션이 유효하지 않습니다."));
    }

    @Test
    @DisplayName("사용자 생성 시 필수 입력값이 누락되면 400을 반환하는가?")
    void validatesCreateUserRequiredFields() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf())
                                         .contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isBadRequest())
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

    @Test
    @DisplayName("기존 계정 생성 경로(/api/v1/admin/users)는 더 이상 매핑되지 않는가?")
    void doesNotMapLegacyCreateUserPath() throws Exception {
        mvc.perform(post("/api/v1/admin/users").with(csrf())
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(mapper.writeValueAsString(joinRequest)))
           .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("사용자 삭제 시 결과 혹은 오류를 반환하는가?")
    void deletesUsersWithSuccessAndErrorStatuses() throws Exception {
        mvc.perform(delete("/api/v1/users/3").with(csrf()))
           .andExpect(status().isOk())
           .andExpect(content().string("회원이 탈퇴되었습니다."));
        doThrow(new AccessDeniedException("forbidden")).when(service).deleteUser(3L);
        mvc.perform(delete("/api/v1/users/3").with(csrf()))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error").value("forbidden"));
        doThrow(new UsernameNotFoundException("missing")).when(service).deleteUser(4L);
        mvc.perform(delete("/api/v1/users/4").with(csrf()))
           .andExpect(status().isNotFound());
        doThrow(new NullPointerException()).when(service).deleteUser(5L);
        mvc.perform(delete("/api/v1/users/5").with(csrf()))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error").value("세션이 유효하지 않습니다."));
    }

    @Test
    @DisplayName("권한 변경 시 결과 혹은 오류를 반환하는가?")
    void updatesRolesWithSuccessAndErrorStatuses() throws Exception {
        mvc.perform(patch("/api/v1/users/3").with(csrf())
                                            .contentType(MediaType.TEXT_PLAIN).content("ADMIN"))
           .andExpect(status().isOk())
           .andExpect(content().string("권한이 수정되었습니다."));
        doThrow(new AccessDeniedException("forbidden")).when(service)
                                                       .updateRole(3L, "ADMIN");
        mvc.perform(patch("/api/v1/users/3").with(csrf())
                                            .contentType(MediaType.TEXT_PLAIN).content("ADMIN"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error").value("forbidden"));
        doThrow(new UsernameNotFoundException("missing")).when(service)
                                                         .updateRole(4L, "ADMIN");
        mvc.perform(patch("/api/v1/users/4").with(csrf())
                                            .contentType(MediaType.TEXT_PLAIN).content("ADMIN"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("missing"));
        doThrow(new IllegalArgumentException("invalid")).when(service)
                                                        .updateRole(5L, "BAD");
        mvc.perform(patch("/api/v1/users/5").with(csrf())
                                            .contentType(MediaType.TEXT_PLAIN).content("BAD"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").value("invalid"));
        doThrow(new NullPointerException()).when(service).updateRole(6L, "ADMIN");
        mvc.perform(patch("/api/v1/users/6").with(csrf())
                                            .contentType(MediaType.TEXT_PLAIN).content("ADMIN"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error").value("세션이 유효하지 않습니다."));
    }
}
