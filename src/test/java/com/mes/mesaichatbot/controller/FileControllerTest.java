package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.service.UserService;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FileController")
@ExtendWith(MockitoExtension.class)
class FileControllerTest {
    @Mock
    FileService fileService;
    @Mock
    UserService userService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FileController(fileService, userService)).build();
    }

    @Test
    @DisplayName("파일 목록 요청 시 목록 혹은 오류를 반환하는가?")
    void listsFilesAndReturnsUnauthorizedWithoutUser() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(null);
        when(fileService.getFiles(any())).thenReturn(java.util.List.of());
        mvc.perform(get("/api/v1/files")).andExpect(status().isOk()).andExpect(content().json("[]"));
        when(userService.getCurrentUserEntity()).thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(get("/api/v1/files")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("파일 업로드 시 성공 혹은 오류를 반환하는가?")
    void uploadsAndMapsInvalidFileOrMissingUser() throws Exception {
        var file = new MockMultipartFile("file", "sample.csv", "text/csv", "a,b".getBytes());
        when(fileService.uploadFile(any(), any())).thenReturn(new FileUploadResponseDto("uploaded", null, java.util.List.of()));
        mvc.perform(multipart("/api/v1/files/upload").file(file)).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("uploaded")).andExpect(jsonPath("$.parsedData").isArray()).andExpect(jsonPath("$.parsedData").isEmpty());
        doThrow(new java.io.IOException("bad file")).when(fileService).uploadFile(any(), any());
        mvc.perform(multipart("/api/v1/files/upload").file(file)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fileData").value((Object) null)).andExpect(jsonPath("$.parsedData").value((Object) null));
        doThrow(new UsernameNotFoundException("missing")).when(fileService).uploadFile(any(), any());
        mvc.perform(multipart("/api/v1/files/upload").file(file)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.fileData").value((Object) null)).andExpect(jsonPath("$.parsedData").value((Object) null));
    }

    @Test
    @DisplayName("파일 삭제 시 결과 혹은 오류를 반환하는가?")
    void deletesAndMapsNotFoundUnauthorizedAndForbidden() throws Exception {
        mvc.perform(delete("/api/v1/files/delete/id")).andExpect(status().isOk()).andExpect(jsonPath("$.fileId").value("id")).andExpect(jsonPath("$.message").isNotEmpty());
        doThrow(new IllegalArgumentException("missing")).when(fileService).delete("missing");
        mvc.perform(delete("/api/v1/files/delete/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("missing"));
        doThrow(new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("no auth")).when(fileService).delete("unauth");
        mvc.perform(delete("/api/v1/files/delete/unauth")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.fileId").value((Object) null)).andExpect(jsonPath("$.message").isNotEmpty());
        doThrow(new AccessDeniedException("forbidden")).when(fileService).delete("forbidden");
        mvc.perform(delete("/api/v1/files/delete/forbidden")).andExpect(status().isForbidden()).andExpect(jsonPath("$.fileId").value((Object) null)).andExpect(jsonPath("$.message").value("forbidden"));
    }
}
