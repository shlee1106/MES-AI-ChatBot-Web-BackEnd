package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import com.mes.mesaichatbot.domain.user.service.UserService;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileMetadataRepository fileMetadataRepository;
    private final UserMemberRepository userMemberRepository;
    private final UserService userService;
    private final DocumentService documentService;

    public FileUploadResponseDto uploadFile(MultipartFile file, LocalDateTime requestedAt)
            throws IOException, NotOfficeXmlFileException, NullPointerException, UsernameNotFoundException {
        // UUID 발급.
        UUID fileId = UUID.randomUUID();
        // 사용자의 정보를 UserService에서 가져옴
        UserMember member = userService.getCurrentUserEntity();
        Company company = member.getCompanyId();

        // 메타데이터 전송을 위해 데이터 추출용 변수 2개 추가 생성
        String originName = file.getOriginalFilename();
        String fileType = StringUtils.getFilenameExtension(originName);

        // DB에 삽입할 새 행 생성.
        FileMetadata originalFile = new FileMetadata(fileId.toString(), company, member, file.getOriginalFilename(), fileType, requestedAt);

        // 데이터 파싱 시도.
        List<UploadDataDetailsDto> parsedData = documentService.parse(file, company.getCompanyId(), fileId.toString(), requestedAt);

        // 업로드 파일의 메타데이터를 MySQL로 전송하는 코드 작성
        fileMetadataRepository.save(originalFile);

        // 업로드된 문서를 MongoDB에 저장.
        // 해당 함수는 비동기이므로 백그라운드에서 실행됨.
        documentService.saveToVectorStore(parsedData, originalFile.getFileType());
        // DTO 반환.
        return FileUploadResponseDto.ok(originalFile, parsedData);
    }

    /**
     * 파일을 DB에서 삭제합니다.
     *
     * @param fileId 삭제할 파일의 일련번호.
     * @throws IllegalArgumentException                   삭제할 파일을 찾을 수 없는 경우 발생합니다.
     * @throws IllegalStateException                      삭제할 파일의 메타데이터 혹은 사용자 정보가 잘못 기록된 경우 발생합니다.
     * @throws AuthenticationCredentialsNotFoundException 사용자 인증 정보가 잘못된 경우 발생합니다.
     * @throws AccessDeniedException                      삭제 권한이 없는 경우 발생합니다.
     */
    @Transactional
    public void delete(String fileId)
            throws IllegalArgumentException,
            IllegalStateException, AuthenticationCredentialsNotFoundException,
            AccessDeniedException {
        // FileID로 파일 메타데이터 조회
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일 ID입니다."));

        // 파일 업로더의 회원번호 및 권한 조회
        UserMember uploader = fileMetadata.getEmployeeId();
        if (uploader == null) {
            throw new IllegalStateException("파일 등록자 정보를 찾을 수 없습니다.");
        }

        Long uploaderEmployeeId = uploader.getEmployeeId();
        UserRole uploaderRole = uploader.getRoleCode();

        // 삭제 요청자의 사원번호 추출 및 유저 정보 조회
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        // 사원번호를 추출
        String subject = auth.getName();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("사용자정보 고유ID 추출실패");
        }

        Long requesterEmployeeId = Long.parseLong(subject.contains("_") ? subject.split("_")[1] : subject);

        // 추출한 사원번호로 유저 조회
        UserMember requester = userMemberRepository.findById(requesterEmployeeId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("삭제 요청자 정보를 찾을 수 없습니다."));
        UserRole requesterRole = requester.getRoleCode();

        // USER는 본인이 등록한 파일만 삭제 가능
        if (requesterRole == UserRole.ROLE_USER) {
            if (!requesterEmployeeId.equals(uploaderEmployeeId)) {
                throw new AccessDeniedException("일반 사원은 본인이 업로드한 파일만 삭제할 수 있습니다.");
            }
        }

        // MANAGER는 MASTER의 파일 삭제 불가
        if (requesterRole == UserRole.ROLE_MANAGER) {
            if (uploaderRole == UserRole.ROLE_MASTER) {
                throw new AccessDeniedException("중간 관리자는 최고 관리자의 파일을 삭제할 수 없습니다.");
            }
        }

        // Mysql에서 메타데이터 삭제
        fileMetadataRepository.delete(fileMetadata);

        // 몽고DB에 등록된 파싱데이터 삭제
        documentService.deleteFromVectorStore(fileId);
        log.info("MySQL 파일 메타데이터 삭제 완료: fileId = {}, 요청자 = {}", fileId, requesterEmployeeId);
    }
}