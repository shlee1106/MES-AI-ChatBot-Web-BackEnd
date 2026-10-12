package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserMemberRepository userMemberRepository;

    @Transactional
    public void delete(String fileId) {
        // FileID로 파일 메타데이터 조회
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파일 ID입니다: "));

        // 파일 업로더의 회원번호 및 권한 조회
        UserMember uploader = fileMetadata.getEmployeeId();
        if (uploader == null) {
            throw new IllegalArgumentException("파일 등록자 정보를 찾을 수 없습니다.");
        }

        Long uploaderEmployeeId = uploader.getEmployeeId();
        UserRole uploaderRole = uploader.getRoleCode();

        // 삭제 요청자의 사원번호 추출 및 유저 정보 조회
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        // 사원번호를 추출
        String subject = auth.getName();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("사용자정보 고유ID 추출실패");
        }

        Long requesterEmployeeId = 0L;

        try {
            requesterEmployeeId = Long.parseLong(subject.contains("_") ? subject.split("_")[1] : subject);
        }catch (Exception e){
            log.error("사원번호 파싱 실패: subject = {}", subject, e);
        }

        // 추출한 사원번호로 유저 조회
        UserMember requester = userMemberRepository.findById(requesterEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("삭제 요청자 정보를 찾을 수 없습니다."));
        UserRole requesterRole = requester.getRoleCode();

        // USER는 본인이 등록한 파일만 삭제 가능
        if (requesterRole == UserRole.ROLE_USER) {
            if (!requesterEmployeeId.equals(uploaderEmployeeId)) {
                throw new IllegalArgumentException("일반 사원은 본인이 업로드한 파일만 삭제할 수 있습니다.");
            }
        }

        // MANAGER는 MASTER의 파일 삭제 불가
        if (requesterRole == UserRole.ROLE_MANAGER) {
            if (uploaderRole == UserRole.ROLE_MASTER) {
                throw new IllegalArgumentException("중간 관리자는 최고 관리자의 파일을 삭제할 수 없습니다.");
            }
        }

        // Mysql에서 메타데이터 삭제
        fileMetadataRepository.delete(fileMetadata);
        log.info("MySQL 파일 메타데이터 삭제 완료: fileId = {}, 요청자 = {}", fileId, requesterEmployeeId);
    }
}