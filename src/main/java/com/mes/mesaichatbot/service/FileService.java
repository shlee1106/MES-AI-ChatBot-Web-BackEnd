package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.dto.FileUploadResponseDto;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.repository.CompanyRepository;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 파일 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileMetadataRepository fileMetadataRepository;
    private final UserMemberRepository userMemberRepository;
    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final DocumentService documentService;

    /**
     * 해당 사용자의 개별 파일 목록(shared = false)을 반환합니다.
     * 메인 페이지에서 중간관리자가 올린 공용 파일이 개인 파일 목록에 섞여 보이지 않도록 필터링합니다.
     *
     * @param member 사용자 정보.
     * @return {@link FileMetadata} 리스트 형식의 파일 목록.
     */
    @Transactional(readOnly = true)
    public List<FileMetadata> getFiles(UserMember member) {
        return fileMetadataRepository.findAllByEmployeeId(member)
                                     .stream()
                                     .filter(f -> !f.isShared())
                                     .toList();
    }

    /**
     * 관리자 페이지 및 조건별 파일 목록을 반환합니다.
     *
     * @param currentUser        요청 사용자 엔티티
     * @param shared             공용 데이터 파일 조회 여부
     * @param employeeId         특정 사원의 개별 파일 조회 시 대상 사원번호
     * @param allCompany         사내 전체 파일 일괄 조회 여부
     * @param requestedCompanyId 최고관리자(ROLE_MASTER)가 선택한 대상 회사일련번호
     * @return 조건에 맞는 {@link FileMetadata} 리스트
     */
    @Transactional(readOnly = true)
    public List<FileMetadata> getFilesForQuery(UserMember currentUser,
                                               Boolean shared,
                                               Long employeeId,
                                               boolean allCompany,
                                               Long requestedCompanyId) {
        Company company = currentUser.getCompany();
        boolean isAdmin = currentUser.getRoleCode() != UserRole.ROLE_USER;

        if (currentUser.getRoleCode() == UserRole.ROLE_MASTER && requestedCompanyId != null) {
            Company targetCompany = companyRepository.findByCompanyId(requestedCompanyId);
            if (targetCompany != null) {
                company = targetCompany;
            }
        }

        if (allCompany && isAdmin) {
            return fileMetadataRepository.findAllFilesByCompany(company);
        }
        if (Boolean.TRUE.equals(shared)) {
            return fileMetadataRepository.findSharedFilesByCompany(company);
        }
        if (employeeId != null && isAdmin) {
            return userMemberRepository.findByCompanyIdAndEmployeeId(company, employeeId)
                                       .map(target -> fileMetadataRepository.findAllByEmployeeId(target)
                                                                            .stream()
                                                                            .filter(f -> !f.isShared())
                                                                            .toList())
                                       .orElseGet(List::of);
        }
        return getFiles(currentUser);
    }

    /**
     * 파일을 업로드합니다 (기본: 현재 로그인 사용자 귀속).
     */
    @Transactional
    public FileUploadResponseDto uploadFile(MultipartFile file, LocalDateTime requestedAt, Boolean shared)
            throws IOException, NotOfficeXmlFileException, UsernameNotFoundException, UnsupportedOperationException {
        return uploadFile(file, requestedAt, Boolean.TRUE.equals(shared), null, null);
    }

    /**
     * 파일을 업로드합니다 (관리자가 targetEmployeeId 또는 targetCompanyId를 지정하여 업로드하는 기능 포함).
     *
     * @param file             업로드할 파일.
     * @param requestedAt      요청 시간.
     * @param shared           공용 데이터 파일 여부.
     * @param targetEmployeeId 대상 사원번호 (null이면 현재 로그인한 사용자 본인).
     * @param targetCompanyId  최고관리자가 선택한 대상 회사일련번호.
     * @return 업로드된 파일 정보.
     */
    @Transactional
    public FileUploadResponseDto uploadFile(MultipartFile file,
                                            LocalDateTime requestedAt,
                                            boolean shared,
                                            Long targetEmployeeId,
                                            Long targetCompanyId)
            throws IOException, NotOfficeXmlFileException, UsernameNotFoundException, UnsupportedOperationException {
        // UUID 발급.
        UUID fileId = UUID.randomUUID();
        // 요청자 정보를 UserService에서 가져옴
        UserMember requester = userService.getCurrentUserEntity();
        Company company = requester.getCompany();

        if (requester.getRoleCode() == UserRole.ROLE_MASTER && targetCompanyId != null) {
            Company foundCompany = companyRepository.findByCompanyId(targetCompanyId);
            if (foundCompany != null) {
                company = foundCompany;
            }
        }

        // 일반 사원은 공용 데이터 파일 업로드 불가
        if (shared && requester.getRoleCode() == UserRole.ROLE_USER) {
            throw new AccessDeniedException("일반 사원은 공용 데이터 파일을 업로드할 권한이 없습니다.");
        }

        // 실제 파일 소유자 결정 (관리자가 targetEmployeeId 또는 타 회사를 지정한 경우 해당 사원에게 귀속)
        UserMember owner = requester;
        if (!shared && targetEmployeeId != null) {
            if (requester.getRoleCode() == UserRole.ROLE_USER && !targetEmployeeId.equals(requester.getEmployeeId())) {
                throw new AccessDeniedException("일반 사원은 타 사원의 파일을 업로드할 권한이 없습니다.");
            }
            owner = userMemberRepository.findByCompanyIdAndEmployeeId(company, targetEmployeeId)
                                        .orElseThrow(() -> new UsernameNotFoundException("대상 사원 정보를 찾을 수 없습니다."));
        } else if (shared && !company.equals(requester.getCompany())) {
            List<UserMember> companyMembers = userMemberRepository.findByCompanyId(company);
            if (!companyMembers.isEmpty()) {
                owner = companyMembers.getFirst();
            }
        }

        // 메타데이터 전송을 위해 데이터 추출용 변수 2개 추가 생성
        String originName = file.getOriginalFilename();
        String normalizedOriginName = originName != null ? originName.trim() : "";
        String fileType = StringUtils.getFilenameExtension(originName);

        // 중복 파일 검사: 공용 파일은 회사 단위로, 개별 파일은 대상 사원 단위로 중복 차단
        if (!normalizedOriginName.isEmpty()) {
            if (shared) {
                if (fileMetadataRepository.existsSharedByCompanyAndOriginName(company, normalizedOriginName)) {
                    throw new DuplicateKeyException("이미 존재하는 파일입니다.");
                }
            } else {
                if (fileMetadataRepository.existsPersonalByEmployeeAndOriginName(owner, normalizedOriginName)) {
                    throw new DuplicateKeyException("이미 업로드한 파일이 존재합니다.");
                }
            }
        }

        // DB에 삽입할 새 행(shared) 생성 (owner 사원에게 귀속)
        FileMetadata originalFile = new FileMetadata(fileId.toString(), owner, file.getOriginalFilename(), fileType,
                                                     shared, requestedAt);

        // 데이터 파싱 시도.
        List<UploadDataDetailsDto> parsedData = documentService.parse(file, company.getCompanyId(), fileId.toString(),
                                                                      requestedAt);

        // 업로드 파일의 메타데이터를 MySQL로 전송하는 코드
        fileMetadataRepository.save(originalFile);

        // 업로드된 문서를 MongoDB에 저장.
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
                                                          .orElseThrow(() -> new IllegalArgumentException(
                                                                  "존재하지 않는 파일 ID입니다."));

        // 파일 업로더의 회원번호 및 권한 조회
        UserMember uploader = fileMetadata.getEmployeeId();
        if (uploader == null) {
            throw new IllegalStateException("파일 등록자 정보를 찾을 수 없습니다.");
        }

        Long uploaderEmployeeId = uploader.getEmployeeId();
        UserRole uploaderRole = uploader.getRoleCode();

        // 삭제 요청자의 사원번호 추출 및 유저 정보 조회
        UserMember requester = userService.getCurrentUserEntity();
        UserRole requesterRole = requester.getRoleCode();

        // USER는 본인이 등록한 파일만 삭제 가능
        if (requesterRole == UserRole.ROLE_USER) {
            if (!requester.getEmployeeId().equals(uploaderEmployeeId)) {
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
        try {
            documentService.deleteFromVectorStore(fileId);
        } catch (UnsupportedOperationException e) {
            log.warn("LLM 서비스를 이용할 수 없습니다: {}", e.getMessage(), e);
        }
        log.info("MySQL 파일 메타데이터 삭제 완료: fileId = {}, 요청자 = {}_{}", fileId, requester.getCompany().getCompanyId(),
                 requester.getEmployeeId());
    }
}
