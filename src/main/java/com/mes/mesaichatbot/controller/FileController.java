package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.dto.FileDeleteResponseDto;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 파일 관련 작업을 처리하는 RestController.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    /**
     * 파일 업로드를 처리합니다.
     *
     * @param file 업로드된 {@link MultipartFile} 파일.
     * @return JSON 형태의 {@link FileUploadResponseDto} 객체.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileUploadResponseDto> upload(@RequestPart("file") MultipartFile file) {
        // 요청 시각 기록.
        LocalDateTime now = LocalDateTime.now();
        try {
            // 성공 시 200 반환.
            // MongoDB 저장 여부와는 상관 없음.
            FileUploadResponseDto dto = fileService.uploadFile(file, now);
            return ResponseEntity.ok(dto);
        } catch (IOException | NotOfficeXmlFileException | NullPointerException e) {
            return ResponseEntity.badRequest().body(new FileUploadResponseDto("파일이 올바르지 않습니다.", null, null));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new FileUploadResponseDto("인증 정보가 올바르지 않습니다.", null, null));
        }
    }

    /**
     * 업로드 된 파일을 삭제합니다.
     *
     * @param fileId 삭제할 파일의 식별자 ID (UUID 문자열)
     * @return 삭제 처리 결과 메시지
     */
    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<FileDeleteResponseDto> delete(@PathVariable("fileId") String fileId) {
        try {
            // 파일 삭제
            fileService.delete(fileId);

            // 완료응답 반환
            return ResponseEntity.ok(FileDeleteResponseDto.of(fileId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new FileDeleteResponseDto(null, e.getMessage(), null));
        } catch (AuthenticationCredentialsNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new FileDeleteResponseDto(null, "인증 정보가 올바르지 않습니다.", null));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new FileDeleteResponseDto(null, e.getMessage(), null));
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new FileDeleteResponseDto(null, "삭제 중 예기치 못한 오류가 발생했습니다.", null));
        }
    }
}
