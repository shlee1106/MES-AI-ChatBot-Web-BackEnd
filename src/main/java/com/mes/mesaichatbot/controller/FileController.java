package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.OriginalFile;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController("/files")
public class FileController {

    /**
     * 문서 파싱 Service.
     */
    private final DocumentService documentService;

    // 자동 주입.
    public FileController(DocumentService documentService) {
        this.documentService = documentService;
    }


    /**
     * 파일 업로드를 처리합니다.
     *
     * @param file 업로드된 {@link MultipartFile} 파일.
     * @return JSON 형태의 FileUploadResponseDto 객체.
     */
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileUploadResponseDto> uploadFile(@RequestPart("file") MultipartFile file, @RequestHeader(name = "Authorization", required = false) String authorization) { // TODO: 인증 시스템 연동 시 required = true로 설정 혹은 지우기.
        // UUID 발급.
        UUID fileId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Company company = new Company(); // TODO: 인증 시스템 연동 시 회사정보 입력
        UserMember employee = new UserMember(); // TODO:  인증 시스템 연동 시 사원정보 입력
        // DB에 삽입할 새 행 생성.
        OriginalFile originalFile = new OriginalFile(fileId.toString(), company, employee, file.getOriginalFilename(), now.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + file.getOriginalFilename(), StringUtils.getFilenameExtension(file.getOriginalFilename()), null, now);
        
        // 데이터 파싱 시도.
        List<UploadDataDetailsDto> parsedData;
        try {
            parsedData = documentService.parse(file, company.getCompanyId(), fileId.toString(), now);
        } catch (IOException | NotOfficeXmlFileException e) {
            // IOException 발생 시 사용자가 잘못된 데이터를 던진 것으로 간주,
            // Bad Request 반환.
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        // 성공 시 200 반환.
        return ResponseEntity.ok(new FileUploadResponseDto(originalFile, parsedData));
    }
}
