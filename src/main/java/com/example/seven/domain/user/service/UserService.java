package com.example.seven.domain.user.service;

import com.example.seven.domain.user.dto.JoinRequestDTO;
import com.example.seven.domain.user.dto.UserInfoResponseDTO;
import com.example.seven.domain.user.entity.UserEntity;
import com.example.seven.domain.user.entity.UserRole;
import com.example.seven.domain.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public void createUser(JoinRequestDTO dto) {
        // 현재 요청을 보낸 사람의 권한 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String creatorRole = auth.getAuthorities().iterator().next().getAuthority();

        // 일반 사원은 계정생성 불가
        if (creatorRole.equals("ROLE_USER")) {
            throw new IllegalArgumentException("일반 사원은 계정을 생성할 권한이 없습니다.");
        }

        // 중간관리자는 최고관리자, 자신과 같은 중간관리자 생성 불가
        if (creatorRole.equals("ROLE_MANAGER")) {
            if(dto.role() == UserRole.ROLE_MASTER || dto.role() == UserRole.ROLE_MANAGER) {
                throw new IllegalArgumentException("중간관리자는 하위관리자 계정만 생성할 수 있습니다.");
            }
        }

        // 계정 생성 중복 검증
        if (userRepository.existsByCompanyIdAndEmployeeId(dto.companyId(), dto.employeeId())) {
            throw new IllegalArgumentException("이미 등록된 사원입니다.");
        }

        // 엔티티 저장
        UserEntity entity = new UserEntity();
        entity.setCompanyId(dto.companyId());
        entity.setEmployeeId(dto.employeeId());
        entity.setUsername(dto.username());
        entity.setEmail(dto.email());
        entity.setPassword(passwordEncoder.encode(dto.password()));
        entity.setRole(dto.role());

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
        // 스프링 시큐리티 버전 호환을 위해 권한 문자열에 ROLE_ 제거 후 전달
        String role = entity.getRole().name().replace("ROLE_", "");

        return User.builder()
                .username(combinedUsername)
                .password(entity.getPassword())
                .roles(role) //.roles() 메서드 내부적으로 다시 ROLE_을 붙여줘 순수 이름만 넘김.
                .build();
    }

    public UserInfoResponseDTO getMyInfo() {
        // 시큐리티 컨택스트에서 JWT토큰의 Subject(예) "1_2000")를 가져옴
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        String[] parts = subject.split("_");
        Long companyId = Long.parseLong(parts[0]); // companyId(1)가져옴
        Long employeeId = Long.parseLong(parts[1]); // employeeId(2000)가져옴

        // DB조회 없으면 예외처리
        UserEntity user = userRepository.findByCompanyIdAndEmployeeId(companyId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // DTO 변환 후 반환
        return new UserInfoResponseDTO(
                user.getCompanyId(),
                user.getEmployeeId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}