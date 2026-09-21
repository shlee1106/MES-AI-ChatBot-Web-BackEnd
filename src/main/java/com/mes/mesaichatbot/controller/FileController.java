package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.user.service.UserService;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import com.mes.mesaichatbot.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    /**
     * 문서 파싱 Service.
     */
    private final DocumentService documentService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserService userService;

    // 자동 주입.
    public FileController(DocumentService documentService, FileMetadataRepository fileMetadataRepository, UserService userService) {
        this.documentService = documentService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.userService = userService;
    }

    /**
     * 파일 업로드를 처리합니다.
     *
     * @param file 업로드된 {@link MultipartFile} 파일.
     * @return JSON 형태의 {@link FileUploadResponseDto} 객체.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @RequestPart("file") MultipartFile file
    ) {
        // UUID 발급.
        UUID fileId = UUID.randomUUID();
        UserMember member = userService.getMyInfo();
        Company company = member.getCompanyId();
        //메타데이터 전송을 위해 밑에 데이터를 추출하기위한 변수 2개 추가생성
        String originName = file.getOriginalFilename();
        String fileType = StringUtils.getFilenameExtension(originName);
        LocalDateTime now = LocalDateTime.now();

        // DB에 삽입할 새 행 생성.
        FileMetadata originalFile = new FileMetadata(fileId.toString(), company, member, file.getOriginalFilename(), fileType, now);

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

        // 업로드 파일의 메타데이터를 Mysql로 전송하는 코드 작성
        try {
            fileMetadataRepository.save(originalFile);
        } catch (Exception e) {
            log.error(String.valueOf(e));
            log.error("파일 메타데이터 DB전송 실패");
        }


        // 업로드된 문서를 MongoDB에 저장.
        // 해당 함수는 비동기이므로 백그라운드에서 실행됨.
        documentService.saveToVectorStore(parsedData, originalFile.getFileType());

        // 성공 시 200 반환.
        // MongoDB 저장 여부와는 상관 없음.
        return ResponseEntity.ok(new FileUploadResponseDto(originalFile, parsedData));
    }
}
