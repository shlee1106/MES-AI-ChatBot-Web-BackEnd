package com.mes.mesaichatbot.service;


import com.mes.mesaichatbot.domain.PasswordResetToken;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.dto.PasswordResetRequestDTO;
import com.mes.mesaichatbot.repository.CompanyRepository;
import com.mes.mesaichatbot.repository.PasswordResetTokenRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserMemberRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final RefreshService refreshService;

    @Value("${app.frontend-hostname}")
    private String hostname;

    @Transactional
    public void sendResetEmail(PasswordResetRequestDTO dto) {
        // DB에 회사번호, 사원번호, 이메일이 모두 일치하는 유저가 있는지 확인
        UserMember user = userRepository.findByCompanyIdAndEmployeeIdAndCompanyEmail(
                companyRepository.findByCompanyId(dto.companyId()), dto.employeeId(), dto.email()
        ).orElseThrow(() -> new IllegalArgumentException("일치하는 사용자 정보가 없습니다."));


        // UUID 토큰 생성 및 Redis에 저장 (5분 뒤 자동 삭제됨)
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user.getCompanyId(),
                                                               user.getEmployeeId());
        tokenRepository.save(resetToken);

        // 프론트엔드 URL과 토큰이 결합된 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getCompanyEmail());
        message.setSubject("[MES AI ChatBot 안내] 비밀번호 재설정 링크 이메일");
        message.setText("""
                                비밀번호 재설정 링크: %s/reset-pw?token=%s
                                
                                (보안을 위해 본 링크는 5분 후 만료됩니다.)""".formatted(hostname, token));
        mailSender.send(message);
    }

    @Transactional(readOnly = true)
    public void verifyTokenExists(String token) {
        if (!tokenRepository.existsById(token)) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 링크입니다.");
        }
    }

    @Transactional
    public void updatePassword(String token, String newPassword) {
        // Redis에서 토큰 조회(5분이 지나 삭제되었으면 에러발생)
        PasswordResetToken resetToken = tokenRepository.findById(token)
                                                       .orElseThrow(() -> new IllegalArgumentException(
                                                               "만료되었거나 유효하지 않은 링크입니다."));

        // 토큰에 저장된 식별자로 MySQL에서 유저 조회
        UserMember user = userRepository.findByCompanyIdAndEmployeeId(
                companyRepository.findByCompanyId(resetToken.getCompanyId()), resetToken.getEmployeeId()
        ).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 비밀번호 암호화 후 업데이트
        user.setPassword(passwordEncoder.encode(newPassword));

        // 비밀번호가 바뀌었으므로 기존에 발급된 Refresh Token을 모두 폐기
        refreshService.revokeAllTokens(String.valueOf(resetToken.getCompanyId()), String.valueOf(resetToken.getEmployeeId()));

        // 사용이 완료된 임시 토큰은 Redis에서 즉시 파기하여 재사용 방지
        tokenRepository.delete(resetToken);
    }
}
