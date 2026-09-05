package com.example.seven.domain.user.service;

import com.example.seven.domain.user.dto.JoinRequestDTO;
import com.example.seven.domain.user.entity.UserEntity;
import com.example.seven.domain.user.entity.UserRole;
import com.example.seven.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void join(JoinRequestDTO dto) {
        // 1. 중복 검증
        if (userRepository.existsByCompanyIdAndEmployeeId(dto.companyId(), dto.employeeId())) {
            throw new IllegalArgumentException("이미 등록된 사원입니다.");
        }

        // 2. 엔티티 저장
        UserEntity entity = new UserEntity();
        entity.setCompanyId(dto.companyId());
        entity.setEmployeeId(dto.employeeId());
        entity.setUsername(dto.username());
        entity.setEmail(dto.email());
        entity.setPassword(passwordEncoder.encode(dto.password()));
        entity.setRole(UserRole.USER);

        userRepository.save(entity);
    }

    @Override
    public UserDetails loadUserByUsername(String combinedUsername) throws UsernameNotFoundException {

        // 1. LoginFilter에서 결합한 문자열("회사번호_사원번호") 분해
        String[] parts = combinedUsername.split("_");
        Long companyId = Long.parseLong(parts[0]);
        Long employeeId = Long.parseLong(parts[1]);

        // 2. DB 조회
        UserEntity entity = userRepository.findByCompanyIdAndEmployeeId(companyId, employeeId)
                .orElseThrow(() -> new UsernameNotFoundException("사원 정보를 찾을 수 없습니다."));

        // 3. Security 인증 객체 반환 (username에 결합된 문자열을 그대로 세팅하여 JWT 페이로드에 활용)
        return User.builder()
                .username(combinedUsername)
                .password(entity.getPassword())
                .roles(entity.getRole().name())
                .build();
    }
}