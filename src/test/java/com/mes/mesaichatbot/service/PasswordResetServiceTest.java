package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.PasswordResetToken;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserRole;
import com.mes.mesaichatbot.repository.CompanyRepository;
import com.mes.mesaichatbot.repository.PasswordResetTokenRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PasswordResetService")
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    UserMemberRepository userRepository;
    @Mock
    PasswordResetTokenRepository tokenRepository;
    @Mock
    JavaMailSender mailSender;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    CompanyRepository companyRepository;
    @Mock
    RefreshService refreshService;
    @InjectMocks
    PasswordResetService service;

    @Test
    @DisplayName("비밀번호 재설정 성공 시 Refresh Token을 폐기하고 재설정 토큰을 삭제하는가?")
    void updatePasswordRevokesRefreshTokensAndDeletesResetToken() {
        Company company = new Company(1L, "MES", null);
        UserMember user = new UserMember(2000L, company, "이름", "old", "a@example.com",
                                         UserRole.ROLE_USER, LocalDateTime.now());
        PasswordResetToken resetToken = new PasswordResetToken("tok", 1L, 2000L);
        when(tokenRepository.findById("tok")).thenReturn(Optional.of(resetToken));
        when(companyRepository.findByCompanyId(1L)).thenReturn(company);
        when(userRepository.findByCompanyIdAndEmployeeId(company, 2000L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword1!")).thenReturn("encodedNew");

        service.updatePassword("tok", "newPassword1!");

        assertEquals("encodedNew", user.getPassword());
        verify(refreshService).revokeAllTokens("1", "2000");
        verify(tokenRepository).delete(resetToken);
    }
}