package com.example.seven.domain.email.serivce;


import com.example.seven.domain.email.dto.PasswordResetRequestDTO;
import com.example.seven.domain.email.dto.PasswordUpdateRequestDTO;
import com.example.seven.domain.email.entity.PasswordResetToken;
import com.example.seven.domain.email.repository.PasswordResetTokenRepository;
import com.example.seven.domain.user.entity.UserEntity;
import com.example.seven.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendRestEmail(PasswordResetRequestDTO dto) {

        // DB에 회사번호, 사원번호, 이메일이 모두 일치하는 유저가 있는지 확인
        UserEntity user = userRepository.findByCompanyIdAndEmployeeIdAndEmail(
                dto.companyId(), dto.employeeId(), dto.email()
        ).orElseThrow(() -> new IllegalArgumentException("일치하는 사용자 정보가 없습니다."));


        // UUID 토큰 생성 및 Redis에 저장 (5분 뒤 자동 삭제됨)
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user.getCompanyId(), user.getEmployeeId());
        tokenRepository.save(resetToken);

        // 프론트엔드 URL과 토큰이 결합된 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        //TODO: 외부IP 자동으로 할당
        message.setSubject("[MES AI ChatBot 안내] 비밀번호 재설정 링크 이메일");
//        message.setText("비밀번호 재설정 링크: https://43.200.89.191:8080/api/v1/password/reset-pw?token=" + token +
//                "\n\n(보안을 위해 본 링크는 5분 후 만료됩니다.)");
        message.setText("비밀번호 재설정 링크: http://localhost:3000/reset-pw?token=" + token +
                "\n\n(보안을 위해 본 링크는 5분 후 만료됩니다.)");
        mailSender.send(message);

    }

    @Transactional(readOnly = true)
    public void verifyTokenExists(String token){
        if (!tokenRepository.existsById(token)) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 링크입니다.");
        }
    }


    @Transactional
    public void updatePassword(String token, String newPassword) {

        // Redis에서 토큰 조회(5분이 지나 삭제되었으면 에러발생)
        PasswordResetToken resetToken = tokenRepository.findById(token)
                .orElseThrow(() -> new IllegalArgumentException("만료되었거나 유효하지 않은 링크입니다."));


        // 토큰에 저장된 식별자로 MySQL에서 유저 조회
        UserEntity user = userRepository.findByCompanyIdAndEmployeeId(
                resetToken.getCompanyId(), resetToken.getEmployeeId()
        ).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 비밀번호 암호화 후 업데이트
        user.setPassword(passwordEncoder.encode(newPassword));

        // 사용이 완료된 임시 토큰은 Redis에서 즉시 파기하여 재사용 방지
        tokenRepository.delete(resetToken);
    }
}
