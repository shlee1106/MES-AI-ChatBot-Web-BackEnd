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

    /**
     * 권한별로 만들 수 있는 유저 생성 메소드
     */
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

    /**
     * Spring Security가 로그인을 처리할 때 '비밀번호 검증'을 위해 자동으로 호출하는 핵심 메서드입니다.
     * LoginFilter에서 전달받은 식별자("회사번호_사원번호")를 이용해 DB에서 유저의 진짜 정보를 꺼내온 뒤,
     * Security가 심사할 수 있도록 공식 규격(UserDetails)으로 포장해서 반환합니다.
     *
     * @param combinedUsername LoginFilter에서 조합하여 넘긴 "회사번호_사원번호" 형태의 식별자 (예: "1_2000")
     * @return DB에서 찾은 유저의 비밀번호와 권한이 담긴 Security 공식 인증 객체
     * @throws UsernameNotFoundException DB에 해당 사원 정보가 존재하지 않을 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String combinedUsername) throws UsernameNotFoundException {

        // 1. LoginFilter에서 '_' 기준으로 결합한 문자열("회사번호_사원번호") 분해
        String[] parts = combinedUsername.split("_");
        Long companyId = Long.parseLong(parts[0]);
        Long employeeId = Long.parseLong(parts[1]);

        // 2. DB 조회: 분리한 회사번호와 사원번호로 우리 회사의 진짜 직원 정보(Entity 원본 장부)를 꺼내옴
        UserEntity entity = userRepository.findByCompanyIdAndEmployeeId(companyId, employeeId)
                .orElseThrow(() -> new UsernameNotFoundException("사원 정보를 찾을 수 없습니다."));

        // 3. Security 권한 규격 맞추기
        // DB엔 "ROLE_USER" 형태로 저장되어 있지만 아래 .roles() 메소드가 내부적으로 "ROLE_"을 붙어주므로
        // 중복("ROLE_ROLE_USER")을 막고자 "ROLE_"을 떼고 순수이름만 남김
        String role = entity.getRole().name().replace("ROLE_", "");

        // 4. 공식 신분증 발급 : DB에서 꺼낸 정보(비밀번호, 권한)를 Security 공식 신분증(User 객체)에 옮겨 적어 제출
        // 제출하면 Security가 알아서 사용자가 로그인 창에 입력한 비밀번호와 DB 비밀번호 비교 진행
        return User.builder()
                .username(combinedUsername) // 나중에 JWT 토큰을 발급할 때 쉽게 꺼내 쓰려고 "1_2000" 형태 그대로 유지
                .password(entity.getPassword()) // [가장 중요] Security가 대조해 볼 DB 속 진짜 암호화된 비밀번호
                .roles(role) //.roles() 메서드 내부적으로 다시 ROLE_을 붙여줘 순수 이름만 넘김.
                .build();
    }

    /**
     * 로그인 한 본인 정보 조회 메소드
     */
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