package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.dto.FileDeleteResponseDto;
import com.mes.mesaichatbot.dto.FileResponseDto;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.service.FileService;
import com.mes.mesaichatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 파일 관련 작업을 처리하는 RestController.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final UserService userService;

    /**
     * 파일 목록을 조회합니다.
     * - 기본 호출: 로그인한 사용자의 개별 파일 목록 반환 (공용 파일 제외)
     * - shared=true: 같은 회사의 공용 데이터 파일 목록 반환
     * - employeeId 지정 (관리자): 해당 사원의 개별 파일 목록 반환
     * - allCompany=true (관리자): 같은 회사의 전체 파일(공용 + 사원별 개별 파일) 반환
     *
     * @return JSON 형태의 {@link FileResponseDto} 객체 리스트.
     */
    @GetMapping
    public ResponseEntity<List<FileResponseDto>> getFiles(
            @RequestParam(required = false) Boolean shared,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false, defaultValue = "false") boolean allCompany,
            @RequestParam(required = false) Long companyId) {
        var currentUser = userService.getCurrentUserEntity();
        if (shared == null && employeeId == null && !allCompany && companyId == null) {
            return ResponseEntity.ok(FileResponseDto.from(fileService.getFiles(currentUser)));
        }
        return ResponseEntity.ok(FileResponseDto.from(
                fileService.getFilesForQuery(currentUser, shared, employeeId, allCompany, companyId)));
    }

    /**
     * 개인 파일 또는 공용 데이터 파일 업로드를 처리합니다.
     *
     * @param file             업로드된 {@link MultipartFile} 파일.
     * @param shared           공용 데이터 파일 여부.
     * @param targetEmployeeId 관리자가 특정 사원의 개별 파일을 대리 업로드할 때 대상 사원번호.
     * @param targetCompanyId  최고관리자가 특정 회사를 선택해 파일을 업로드할 때 대상 회사일련번호.
     * @return JSON 형태의 {@link FileUploadResponseDto} 객체.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FileUploadResponseDto>> upload(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(required = false, defaultValue = "false") boolean shared,
            @RequestParam(required = false) Long targetEmployeeId,
            @RequestParam(required = false) Long targetCompanyId) {

        try {
            // 요청 시각 기록.
            LocalDateTime now = LocalDateTime.now();
            if (file != null) {
                // 성공 시 200 반환.
                // MongoDB 저장 여부와는 상관 없음.
                FileUploadResponseDto responseDto = (targetEmployeeId == null && targetCompanyId == null)
                        ? fileService.uploadFile(file, now, shared)
                        : fileService.uploadFile(file, now, shared, targetEmployeeId, targetCompanyId);
                return ResponseEntity.ok(List.of(responseDto));
            } else if (files != null) {
                List<FileUploadResponseDto> responseDto =
                        files.stream()
                             .map(f -> {
                                 try {
                                     return (targetEmployeeId == null && targetCompanyId == null) ?
                                             fileService.uploadFile(
                                                     f, now,
                                                     shared)
                                             : fileService.uploadFile(
                                             f,
                                             now,
                                             shared,
                                             targetEmployeeId,
                                             targetCompanyId);
                                 } catch (IOException e) {
                                     throw new IllegalArgumentException(e);
                                 }

                             }).toList();
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.badRequest()
                                     .body(List.of(new FileUploadResponseDto(
                                             "파일이 올바르지 않습니다.", null, null)));
            }
        } catch (DuplicateKeyException e) {
            // 중복된 파일명 업로드 시 409 Conflict 반환
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(List.of(new FileUploadResponseDto(
                                         e.getMessage(), null, null)));
        } catch (AccessDeniedException e) {
            // 권한이 부족할 경우 403 Forbidden 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(List.of(new FileUploadResponseDto(
                                         e.getMessage(), null, null)));
        } catch (IOException | NotOfficeXmlFileException e) {
            // 파일이 올바르지 않으면 400 반환
            return ResponseEntity.badRequest().body(
                    List.of(new FileUploadResponseDto(
                            "파일이 올바르지 않습니다.", null, null)));
        }
    }

    /**
     * 업로드 된 파일을 삭제합니다.
     *
     * @param fileId 삭제할 파일의 식별자 ID (UUID 문자열)
     * @return 삭제 처리 결과 메시지
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<FileDeleteResponseDto> delete(@PathVariable String fileId) {
        try {
            // 파일 삭제
            fileService.delete(fileId);

            // 완료응답 반환
            return ResponseEntity.ok(FileDeleteResponseDto.of(fileId));
        } catch (IllegalArgumentException e) {
            // 파일이 존재하지 않으면 404 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(new FileDeleteResponseDto(null, e.getMessage(), null));
        } catch (AuthenticationCredentialsNotFoundException e) {
            // 세션이 유효하지 않으면 401 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(new FileDeleteResponseDto(
                                         null, "인증 정보가 올바르지 않습니다.", null));
        } catch (AccessDeniedException e) {
            // 본인의 파일이 아닐 경우 403 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(new FileDeleteResponseDto(null, e.getMessage(), null));
        } catch (Exception e) {
            // 예기치 못한 오류 발생 시 500 반환
            log.error("파일 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                                 .body(new FileDeleteResponseDto(
                                         null, "삭제 중 예기치 못한 오류가 발생했습니다.",
                                         null));
        }
    }
}
