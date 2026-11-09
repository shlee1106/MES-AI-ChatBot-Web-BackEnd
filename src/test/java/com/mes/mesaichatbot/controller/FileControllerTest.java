package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.config.MockMvcTestConfig;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.service.FileService;
import com.mes.mesaichatbot.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.isNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@WithMockUser(username = "admin", roles = "MASTER")
@Import(MockMvcTestConfig.class)
class FileControllerTest {
    @MockitoBean
    FileService fileService;
    @MockitoBean
    UserService userService;
    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("파일 목록 요청 시 목록 혹은 오류를 반환하는가?")
    void listsFilesAndReturnsUnauthorizedWithoutUser() throws Exception {
        when(userService.getCurrentUserEntity()).thenReturn(null);
        when(fileService.getFiles(any())).thenReturn(java.util.List.of());
        mvc.perform(get("/api/v1/files"))
           .andExpect(status().isOk()).andExpect(content().json("[]"));
        when(userService.getCurrentUserEntity()).thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(get("/api/v1/files"))
           .andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("공유 파일 업로드 시 성공적으로 처리되고 true가 서비스로 전달되는가?")
    void uploadsSharedFileSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.csv", "text/csv", "a,b".getBytes());
        when(fileService.uploadFile(any(), any(), eq(true))).thenReturn(
                new FileUploadResponseDto("uploaded", null, java.util.List.of()));

        mvc.perform(multipart("/api/v1/files")
                            .file(file)
                            .param("shared", "true")
                            .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].message").value("uploaded"))
           .andExpect(jsonPath("$[0].parsedData").isArray())
           .andExpect(jsonPath("$[0].parsedData").isEmpty());

        verify(fileService).uploadFile(any(), any(), eq(true));
    }

    @Test
    @DisplayName("파일 업로드 시 성공 혹은 오류를 반환하는가?")
    void uploadsAndMapsInvalidFileOrMissingUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.csv", "text/csv", "a,b".getBytes());
        when(fileService.uploadFile(any(), any(), any())).thenReturn(
                new FileUploadResponseDto("uploaded", null, java.util.List.of()));
        mvc.perform(multipart("/api/v1/files").file(file).param("shared", "false").with(csrf()))
           .andExpect(status().isOk()).andExpect(jsonPath("$[0].message").value("uploaded"))
           .andExpect(jsonPath("$[0].parsedData").isArray())
           .andExpect(jsonPath("$[0].parsedData").isEmpty());
        doThrow(new java.io.IOException("bad file")).when(fileService).uploadFile(any(), any(), any());
        mvc.perform(multipart("/api/v1/files").file(file).param("shared", "false").with(csrf()))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$[0].fileData").value((Object) null))
           .andExpect(jsonPath("$[0].parsedData").value((Object) null));
        doThrow(new UsernameNotFoundException("missing")).when(fileService).uploadFile(any(), any(), any());
        mvc.perform(multipart("/api/v1/files").file(file).param("shared", "false").with(csrf()))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$[0].fileData").value((Object) null))
           .andExpect(jsonPath("$[0].parsedData").value((Object) null));
    }

    @Test
    @DisplayName("다중 파일 업로드 시 파일 수만큼 결과를 반환하는가?")
    void uploadsMultipleFilesWithoutTarget() throws Exception {
        MockMultipartFile first = new MockMultipartFile(
                "files", "a.csv", "text/csv", "a,b".getBytes());
        MockMultipartFile second = new MockMultipartFile(
                "files", "b.csv", "text/csv", "c,d".getBytes());
        when(fileService.uploadFile(any(), any(), anyBoolean())).thenReturn(
                new FileUploadResponseDto("uploaded", null, java.util.List.of()));

        mvc.perform(multipart("/api/v1/files")
                            .file(first)
                            .file(second)
                            .param("shared", "true")
                            .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(2))
           .andExpect(jsonPath("$[0].message").value("uploaded"))
           .andExpect(jsonPath("$[1].message").value("uploaded"));

        verify(fileService, times(2)).uploadFile(any(), any(), eq(true));
        verify(fileService, never()).uploadFile(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("다중 파일 업로드 시 대상 사원번호 또는 회사번호가 있으면 대리 업로드로 간주하는가?")
    void uploadsMultipleFilesWithTarget() throws Exception {
        MockMultipartFile first = new MockMultipartFile(
                "files", "a.csv", "text/csv", "a,b".getBytes());
        MockMultipartFile second = new MockMultipartFile(
                "files", "b.csv", "text/csv", "c,d".getBytes());
        when(fileService.uploadFile(any(), any(), anyBoolean(), any(), any())).thenReturn(
                new FileUploadResponseDto("uploaded", null, java.util.List.of()));

        mvc.perform(multipart("/api/v1/files")
                            .file(first)
                            .file(second)
                            .param("targetEmployeeId", "7")
                            .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(2));
        verify(fileService, times(2)).uploadFile(any(), any(), eq(false), eq(7L), isNull());

        mvc.perform(multipart("/api/v1/files")
                            .file(first)
                            .param("shared", "true")
                            .param("targetCompanyId", "3")
                            .with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1));
        verify(fileService).uploadFile(any(), any(), eq(true), isNull(), eq(3L));
        verify(fileService, never()).uploadFile(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("파일 파트가 없으면 400을 반환하고 서비스를 호출하지 않는가?")
    void returnsBadRequestWhenNoFilePart() throws Exception {
        mvc.perform(multipart("/api/v1/files").param("shared", "false").with(csrf()))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$[0].message").value("파일이 올바르지 않습니다."))
           .andExpect(jsonPath("$[0].fileData").value((Object) null))
           .andExpect(jsonPath("$[0].parsedData").value((Object) null));

        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("다중 파일 업로드 중 예외가 발생하면 올바른 상태 코드를 반환하는가?")
    void mapsExceptionsOnMultipleFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "a.csv", "text/csv", "a,b".getBytes());
        doThrow(new DuplicateKeyException("duplicated")).when(fileService).uploadFile(any(), any(), anyBoolean());
        mvc.perform(multipart("/api/v1/files").file(file).with(csrf()))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$[0].message").value("duplicated"));
        doThrow(new AccessDeniedException("forbidden")).when(fileService).uploadFile(any(), any(), anyBoolean());
        mvc.perform(multipart("/api/v1/files").file(file).with(csrf()))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$[0].message").value("forbidden"));
        doThrow(new NullPointerException("null")).when(fileService).uploadFile(any(), any(), anyBoolean());
        mvc.perform(multipart("/api/v1/files").file(file).with(csrf()))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$[0].message").value("파일이 올바르지 않습니다."));
        doThrow(new UsernameNotFoundException("missing")).when(fileService).uploadFile(any(), any(), anyBoolean());
        mvc.perform(multipart("/api/v1/files").file(file).with(csrf()))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$[0].message").value("인증 정보가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("파일 삭제 시 결과 혹은 오류를 반환하는가?")
    void deletesAndMapsNotFoundUnauthorizedAndForbidden() throws Exception {
        mvc.perform(delete("/api/v1/files/id").with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.fileId").value("id"))
           .andExpect(jsonPath("$.message").isNotEmpty());
        doThrow(new IllegalArgumentException("missing")).when(fileService).delete("missing");
        mvc.perform(delete("/api/v1/files/missing").with(csrf()))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.message").value("missing"));
        doThrow(new AuthenticationCredentialsNotFoundException("no auth")).when(fileService).delete("unauth");
        mvc.perform(delete("/api/v1/files/unauth").with(csrf()))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.fileId").value((Object) null))
           .andExpect(jsonPath("$.message").isNotEmpty());
        doThrow(new AccessDeniedException("forbidden")).when(fileService).delete("forbidden");
        mvc.perform(delete("/api/v1/files/forbidden").with(csrf()))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.fileId").value((Object) null))
           .andExpect(jsonPath("$.message").value("forbidden"));
    }
}
