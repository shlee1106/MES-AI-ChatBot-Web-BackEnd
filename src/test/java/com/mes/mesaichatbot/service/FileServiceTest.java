package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.repository.CompanyRepository;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock
    FileMetadataRepository fileMetadataRepository;
    @Mock
    UserMemberRepository userMemberRepository;
    @Mock
    CompanyRepository companyRepository;
    @Mock
    UserService userService;
    @Mock
    DocumentService documentService;
    @InjectMocks
    FileService fileService;

    private final Company companyA = new Company(1L, "A사", null);
    private final Company companyB = new Company(2L, "B사", null);

    /**
     * 테스트용 사용자를 생성합니다.
     *
     * @param employeeId 사원번호.
     * @param company    소속 회사.
     * @param role       권한.
     * @return 생성된 {@link UserMember}.
     */
    private UserMember member(Long employeeId, Company company, UserRole role) {
        return new UserMember(employeeId, company, "이름", "encoded", employeeId + "@example.com", role, LocalDateTime.now());
    }

    /**
     * 테스트용 파일 메타데이터를 생성합니다.
     *
     * @param owner 파일 소유자.
     * @return 생성된 {@link FileMetadata}.
     */
    private FileMetadata file(UserMember owner) {
        return new FileMetadata("file-1", owner, "a.csv", "csv", false, LocalDateTime.now());
    }

    @Test
    @DisplayName("사원번호는 같지만 회사가 다른 일반 사원이 삭제하면 권한 예외가 발생하는가?")
    void userCannotDeleteFileOfSameEmployeeIdInOtherCompany() {
        UserMember owner = member(2000L, companyA, UserRole.ROLE_USER);
        UserMember requester = member(2000L, companyB, UserRole.ROLE_USER);
        FileMetadata metadata = file(owner);
        when(fileMetadataRepository.findById("file-1")).thenReturn(Optional.of(metadata));
        when(userService.getCurrentUserEntity()).thenReturn(requester);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> fileService.delete("file-1"));

        assertEquals("일반 사원은 본인이 업로드한 파일만 삭제할 수 있습니다.", ex.getMessage());
        verify(fileMetadataRepository, never()).delete(any(FileMetadata.class));
        verify(documentService, never()).deleteFromVectorStore(anyString());
    }

    @Test
    @DisplayName("일반 사원이 본인 파일을 삭제하면 메타데이터와 벡터 저장소에서 삭제되는가?")
    void userCanDeleteOwnFile() {
        UserMember owner = member(2000L, companyA, UserRole.ROLE_USER);
        FileMetadata metadata = file(owner);
        when(fileMetadataRepository.findById("file-1")).thenReturn(Optional.of(metadata));
        when(userService.getCurrentUserEntity()).thenReturn(owner);

        fileService.delete("file-1");

        verify(fileMetadataRepository).delete(metadata);
        verify(documentService).deleteFromVectorStore("file-1");
    }

    @Test
    @DisplayName("타 회사 중간 관리자가 파일을 삭제하면 권한 예외가 발생하는가?")
    void managerCannotDeleteFileOfOtherCompany() {
        UserMember owner = member(2000L, companyA, UserRole.ROLE_USER);
        UserMember requester = member(3000L, companyB, UserRole.ROLE_MANAGER);
        FileMetadata metadata = file(owner);
        when(fileMetadataRepository.findById("file-1")).thenReturn(Optional.of(metadata));
        when(userService.getCurrentUserEntity()).thenReturn(requester);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> fileService.delete("file-1"));

        assertEquals("타 회사의 파일은 삭제할 수 없습니다.", ex.getMessage());
        verify(fileMetadataRepository, never()).delete(any(FileMetadata.class));
    }

    @Test
    @DisplayName("같은 회사 중간 관리자가 일반 사원 파일을 삭제하면 성공하는가?")
    void managerCanDeleteUserFileOfSameCompany() {
        UserMember owner = member(2000L, companyA, UserRole.ROLE_USER);
        UserMember requester = member(3000L, companyA, UserRole.ROLE_MANAGER);
        FileMetadata metadata = file(owner);
        when(fileMetadataRepository.findById("file-1")).thenReturn(Optional.of(metadata));
        when(userService.getCurrentUserEntity()).thenReturn(requester);

        fileService.delete("file-1");

        verify(fileMetadataRepository).delete(metadata);
        verify(documentService).deleteFromVectorStore("file-1");
    }

    @Test
    @DisplayName("같은 회사 중간 관리자가 최고 관리자 파일을 삭제하면 권한 예외가 발생하는가?")
    void managerCannotDeleteMasterFileOfSameCompany() {
        UserMember owner = member(1000L, companyA, UserRole.ROLE_MASTER);
        UserMember requester = member(3000L, companyA, UserRole.ROLE_MANAGER);
        FileMetadata metadata = file(owner);
        when(fileMetadataRepository.findById("file-1")).thenReturn(Optional.of(metadata));
        when(userService.getCurrentUserEntity()).thenReturn(requester);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> fileService.delete("file-1"));

        assertEquals("중간 관리자는 최고 관리자의 파일을 삭제할 수 없습니다.", ex.getMessage());
        verify(fileMetadataRepository, never()).delete(any(FileMetadata.class));
    }

    @Test
    @DisplayName("최고 관리자는 타 회사 파일도 삭제할 수 있는가?")
    void masterCanDeleteFileOfOtherCompany() {
        UserMember owner = member(2000L, companyB, UserRole.ROLE_USER);
        UserMember requester = member(1000L, companyA, UserRole.ROLE_MASTER);
        FileMetadata metadata = file(owner);
        when(fileMetadataRepository.findById("file-1")).thenReturn(Optional.of(metadata));
        when(userService.getCurrentUserEntity()).thenReturn(requester);

        fileService.delete("file-1");

        verify(fileMetadataRepository).delete(metadata);
        verify(documentService).deleteFromVectorStore("file-1");
    }

    @Test
    @DisplayName("존재하지 않는 파일 ID로 삭제하면 IllegalArgumentException이 발생하는가?")
    void deleteThrowsWhenFileNotFound() {
        when(fileMetadataRepository.findById("none")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> fileService.delete("none"));

        verify(fileMetadataRepository, never()).delete(any(FileMetadata.class));
    }
}