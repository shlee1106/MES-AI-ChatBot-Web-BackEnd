package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.dto.JoinRequestDTO;
import com.mes.mesaichatbot.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.repository.CompanyRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private final Company company1 = new Company(1L, "MES", null);
    private final Company company2 = new Company(2L, "OTHER", null);

    @Mock
    UserMemberRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    CompanyRepository companyRepository;
    @InjectMocks
    UserService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 사원을 생성합니다.
     */
    private UserMember newUser(Long employeeId, Company company, UserRole role) {
        return new UserMember(employeeId, company, "이름", "encoded", "a@example.com", role, LocalDateTime.now());
    }

    /**
     * 현재 사용자를 설정하고, 현재 사용자 조회에 필요한 스텁을 등록합니다.
     * tokenRole은 토큰(Authentication)에 담긴 권한이고, dbUser는 DB에 저장된 사원 정보입니다.
     */
    private void loginAs(UserMember dbUser, String tokenRole) {
        String subject = dbUser.getCompanyId() + "_" + dbUser.getEmployeeId();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(subject, null, List.of(new SimpleGrantedAuthority(tokenRole))));
        when(companyRepository.findByCompanyId(dbUser.getCompanyId())).thenReturn(dbUser.getCompany());
        when(userRepository.findByCompanyIdAndEmployeeId(dbUser.getCompany(), dbUser.getEmployeeId()))
                .thenReturn(Optional.of(dbUser));
    }

    @Test
    @DisplayName("사원 정보가 없으면(탈퇴자 포함) loadUserByUsername이 예외를 던지는가?")
    void throwsWhenUserNotFoundOrDeleted() {
        when(companyRepository.findByCompanyId(1L)).thenReturn(company1);
        when(userRepository.findByCompanyIdAndEmployeeId(company1, 2000L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("1_2000"));
    }

    @Test
    @DisplayName("중간관리자가 회사번호를 지정해 삭제하면 자기 회사 번호여도 거부되는가?")
    void managerCannotSpecifyCompanyOnDelete() {
        loginAs(newUser(2000L, company1, UserRole.ROLE_MANAGER), "ROLE_MANAGER");

        assertThrows(AccessDeniedException.class, () -> service.deleteUser(3000L, 1L));
    }

    @Test
    @DisplayName("하위관리자가 삭제를 시도하면 권한 예외가 발생하는가?")
    void userCannotDelete() {
        loginAs(newUser(2001L, company1, UserRole.ROLE_USER), "ROLE_USER");

        assertThrows(AccessDeniedException.class, () -> service.deleteUser(3000L, null));
    }

    @Test
    @DisplayName("중간관리자가 같은 회사 하위관리자를 삭제하면 탈퇴 처리되는가?")
    void managerDeletesSameCompanyUser() {
        loginAs(newUser(2000L, company1, UserRole.ROLE_MANAGER), "ROLE_MANAGER");
        UserMember target = newUser(3000L, company1, UserRole.ROLE_USER);
        when(userRepository.findByCompanyIdAndEmployeeId(company1, 3000L)).thenReturn(Optional.of(target));

        service.deleteUser(3000L, null);

        assertTrue(target.isDeleted());
    }

    @Test
    @DisplayName("최고관리자가 회사번호를 지정해 타사 사원을 삭제하면 해당 회사에서 조회해 탈퇴 처리되는가?")
    void masterDeletesUserOfOtherCompany() {
        loginAs(newUser(2000L, company1, UserRole.ROLE_MASTER), "ROLE_MASTER");
        when(companyRepository.findByCompanyId(2L)).thenReturn(company2);
        UserMember target = newUser(3000L, company2, UserRole.ROLE_USER);
        when(userRepository.findByCompanyIdAndEmployeeId(company2, 3000L)).thenReturn(Optional.of(target));

        service.deleteUser(3000L, 2L);

        verify(userRepository).findByCompanyIdAndEmployeeId(company2, 3000L);
        assertTrue(target.isDeleted());
    }

    @Test
    @DisplayName("최고관리자가 회사번호를 지정해 권한을 변경하면 권한이 바뀌는가?")
    void masterUpdatesRoleOfOtherCompanyUser() {
        loginAs(newUser(2000L, company1, UserRole.ROLE_MASTER), "ROLE_MASTER");
        when(companyRepository.findByCompanyId(2L)).thenReturn(company2);
        UserMember target = newUser(3000L, company2, UserRole.ROLE_USER);
        when(userRepository.findByCompanyIdAndEmployeeId(company2, 3000L)).thenReturn(Optional.of(target));

        service.updateRole(3000L, 2L, "ROLE_MANAGER");

        assertEquals(UserRole.ROLE_MANAGER, target.getRoleCode());
    }

    @Test
    @DisplayName("중간관리자가 권한을 변경하려 하면 권한 예외가 발생하는가?")
    void managerCannotUpdateRole() {
        loginAs(newUser(2000L, company1, UserRole.ROLE_MANAGER), "ROLE_MANAGER");

        assertThrows(AccessDeniedException.class, () -> service.updateRole(3000L, null, "ROLE_USER"));
    }

    @Test
    @DisplayName("토큰 권한은 최고관리자지만 DB 권한이 하위관리자이면 사원 생성 시 권한 예외가 발생하는가?")
    void createUserUsesDbRoleNotTokenRole() {
        loginAs(newUser(2001L, company1, UserRole.ROLE_USER), "ROLE_MASTER");
        JoinRequestDTO dto = new JoinRequestDTO(1L, 3000L, "Password1!",
                                                "홍길동", "new@example.com", UserRole.ROLE_USER);

        assertThrows(AccessDeniedException.class, () -> service.createUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("최고관리자가 관리자 목록을 조회하면 탈퇴하지 않은 사원만 조회하는가?")
    void masterReadsOnlyNotDeletedUsers() {
        loginAs(newUser(2000L, company1, UserRole.ROLE_MASTER), "ROLE_MASTER");
        when(userRepository.findAllByDeletedFalse())
                .thenReturn(List.of(newUser(3000L, company1, UserRole.ROLE_USER)));

        List<UserInfoResponseDTO> result = service.getUserForAdmin();

        assertEquals(1, result.size());
        verify(userRepository).findAllByDeletedFalse();
    }
}