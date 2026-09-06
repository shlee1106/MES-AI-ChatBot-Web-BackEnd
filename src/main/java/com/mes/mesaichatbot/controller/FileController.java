package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.OriginalFile;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController("/files")
public class FileController {
    /**
     * 파일 업로드를 처리합니다.
     *
     * @param file 업로드된 멀티파트 파일.
     * @return JSON 형태의 FileUploadResponseDto 객체.
     */
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileUploadResponseDto> uploadFile(@RequestPart("file") MultipartFile file, @RequestHeader(name = "Authorization", required = false) String authorization) {
        // UUID 발급.
        UUID fileId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Company company = new Company();
        UserMember employee = new UserMember();
        try {
            TikaDocumentReader reader = new TikaDocumentReader(new ByteArrayResource(file.getBytes()));
            // DB에 삽입할 새 행 생성.
            OriginalFile originalFile = new OriginalFile();
            originalFile.setUploadedAt(now);
            originalFile.setFileId(fileId.toString());
            originalFile.setOriginName(file.getOriginalFilename());
            originalFile.setSaveFileName(now.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + file.getOriginalFilename());
            originalFile.setFileType(StringUtils.getFilenameExtension(file.getOriginalFilename()));
            originalFile.setCompanyId(company); // TODO: 인증 시스템 연동 시 회사정보 입력
            originalFile.setEmployeeId(employee); // TODO:  인증 시스템 연동 시 사원정보 입력

            List<UploadDataDetailsDto> parsedData = new ArrayList<>();
            List<Document> document = reader.get();
            for (int i = 0; i < document.size(); i++) {
                UploadDataDetailsDto dataDetailsDto = new UploadDataDetailsDto(i, company.getCompanyId(), fileId.toString(), i, document.get(i).getText(), now);
                parsedData.add(dataDetailsDto);
            }
            return ResponseEntity.ok(new FileUploadResponseDto(originalFile, parsedData));
        } catch (IOException e) { // 잘못된 파일이 입력되면 Bad Request 반환.
            return ResponseEntity.badRequest().build();
        }
    }
}
