package com.mes.mesaichatbot.domain.user.service;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.user.dto.JoinRequestDTO;
import com.mes.mesaichatbot.domain.user.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import com.mes.mesaichatbot.repository.CompanyRepository;
import com.mes.mesaichatbot.repository.UserMemberRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 사용자 관련 서비스
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserMemberRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;

    /**
     * 권한별로 만들 수 있는 유저 생성 메소드.
     *
     * @param dto 생성할 사원 정보 DTO
     * @throws AccessDeniedException 권한이 없거나 타 회사 소속 직원을 등록하려 할 경우 발생 (403)
     * @throws DuplicateKeyException 이미 등록된 사원번호일 경우 발생 (409)
     */
    @Transactional
    public void createUser(JoinRequestDTO dto) {
        // 현재 요청을 보낸 사람의 권한 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String creatorRole = Objects.requireNonNull(auth).getAuthorities().iterator().next().getAuthority();
        Long creatorCompanyId = Long.parseLong(auth.getName().split("_")[0]);
        Company company = companyRepository.findByCompanyId(dto.companyId());

        // 일반 사원은 계정생성 불가 (권한 부족이므로 403 AccessDeniedException 발생)
        if (Objects.requireNonNull(creatorRole).equals("ROLE_USER")) {
            throw new AccessDeniedException("일반 사원은 계정을 생성할 권한이 없습니다.");
        }

        // 중간관리자는 최고관리자, 자신과 같은 중간관리자 생성 불가 및 타 회사 사원 등록 불가 (403)
        if (creatorRole.equals("ROLE_MANAGER")) {
            if (dto.role() == UserRole.ROLE_MASTER || dto.role() == UserRole.ROLE_MANAGER) {
                throw new AccessDeniedException("중간관리자는 하위관리자 계정만 생성할 수 있습니다.");
            }
            if (!creatorCompanyId.equals(dto.companyId())) {
                throw new AccessDeniedException("중간 관리자는 본인 소속 회사 직원만 등록이 가능합니다.");
            }
        }

        // 계정 생성 중복 검증 (중복 데이터 충돌이므로 409 DuplicateKeyException 발생)
        if (userRepository.existsByCompanyIdAndEmployeeId(company, dto.employeeId())) {
            throw new DuplicateKeyException("이미 등록된 사원입니다.");
        }

        // 엔티티 저장
        UserMember entity = new UserMember(
                dto.employeeId(), company, dto.username(),
                dto.email(), passwordEncoder.encode(dto.password()),
                dto.role(), LocalDateTime.now());

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
    public @NonNull UserDetails loadUserByUsername(String combinedUsername) throws UsernameNotFoundException {

        // 1. LoginFilter에서 '_' 기준으로 결합한 문자열("회사번호_사원번호") 분해
        String[] parts = combinedUsername.split("_");
        Long companyId = Long.parseLong(parts[0]);
        Long employeeId = Long.parseLong(parts[1]);

        // 2. DB 조회: 분리한 회사번호와 사원번호로 우리 회사의 진짜 직원 정보(Entity 원본 장부)를 꺼내옴
        UserMember entity = userRepository.findByCompanyIdAndEmployeeId(companyRepository.findByCompanyId(companyId), employeeId)
                .orElseThrow(() -> new UsernameNotFoundException("사원 정보를 찾을 수 없습니다."));

        // 3. Security 권한 규격 맞추기
        // DB엔 "ROLE_USER" 형태로 저장되어 있지만 아래 .roles() 메소드가 내부적으로 "ROLE_"을 붙어주므로
        // 중복("ROLE_ROLE_USER")을 막고자 "ROLE_"을 떼고 순수이름만 남김
        String role = entity.getRoleCode().name().replace("ROLE_", "");

        // 4. 공식 신분증 발급 : DB에서 꺼낸 정보(비밀번호, 권한)를 Security 공식 신분증(User 객체)에 옮겨 적어 제출
        // 제출하면 Security가 알아서 사용자가 로그인 창에 입력한 비밀번호와 DB 비밀번호 비교 진행
        return User.builder()
                .username(combinedUsername) // 나중에 JWT 토큰을 발급할 때 쉽게 꺼내 쓰려고 "1_2000" 형태 그대로 유지
                .password(entity.getPassword()) // [가장 중요] Security가 대조해 볼 DB 속 진짜 암호화된 비밀번호
                .roles(role) //.roles() 메서드 내부적으로 다시 ROLE_을 붙여줘 순수 이름만 넘김.
                .build();
    }

    /**
     * 현재 사용자의 엔티티 객체를 반환합니다.
     *
     * @return 현재 사용자 정보가 담긴 {@link UserMember} 객체.
     * @throws UsernameNotFoundException 사용자 정보를 찾을 수 없는 경우 발생합니다.
     * @throws NullPointerException      세션이 유효하지 않은 경우 발생합니다.
     */
    @Transactional(readOnly = true)
    public UserInfoResponseDTO getMyInfo() throws UsernameNotFoundException, NullPointerException {
        // 시큐리티 컨택스트에서 JWT토큰의 Subject(예) "1_2000")를 가져옴
        String subject = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        String[] parts = subject.split("_");
        Long companyId = Long.parseLong(parts[0]); // companyId(1)가져옴
        Long employeeId = Long.parseLong(parts[1]); // employeeId(2000)가져옴

        UserMember user = userRepository.findByCompanyIdAndEmployeeId(companyRepository.findByCompanyId(companyId), employeeId)
                .orElseThrow(() -> new UsernameNotFoundException("사원 정보를 찾을 수 없음"));

        // 사용자 정보 반환
        return UserInfoResponseDTO.from(user);
    }

    /**
     * 현재 인증된 사용자의 정보를 반환합니다.
     *
     * @return {@link UserMember}
     * @throws NullPointerException      인증되지 않음
     * @throws UsernameNotFoundException 사용자 정보를 찾을 수 없음
     */
    @Transactional(readOnly = true)
    public UserMember getCurrentUserEntity() throws NullPointerException, UsernameNotFoundException {
        // 세션에서 사용자 정보를 가져옴
        String subject = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        // 사용자 정보에서 회사번호와 사원번호를 추출
        String[] parts = subject.split("_");
        Long companyId = Long.parseLong(parts[0]);
        Long employeeId = Long.parseLong(parts[1]);

        // 사용자 엔티티 반환
        return userRepository.findByCompanyIdAndEmployeeId(companyRepository.findByCompanyId(companyId), employeeId)
                .orElseThrow(() -> new UsernameNotFoundException("사원 정보를 찾을 수 없음"));
    }

    /**
     * 관리자용 사용자 목록을 반환합니다.
     *
     * @return 모든 사용자의 목록이 담긴 {@link List}.
     * @throws NullPointerException  세션이 유효하지 않은 경우 발생합니다.
     * @throws AccessDeniedException 일반 사원(ROLE_USER)이 관리자용 목록 조회를 시도할 경우 발생합니다.
     */
    @Transactional(readOnly = true)
    public List<UserInfoResponseDTO> getUserForAdmin() throws NullPointerException, AccessDeniedException {
        // 인증 정보 취득
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserRole = Objects.requireNonNull(auth).getAuthorities().iterator().next().getAuthority();
        Long currentCompanyId = Long.parseLong(auth.getName().split("_")[0]);

        // 일반 사원(ROLE_USER)은 전체 회원 목록 조회 권한이 없으므로 403 예외 발생
        if ("ROLE_USER".equals(currentUserRole)) {
            throw new AccessDeniedException("일반 사원은 사용자 목록을 조회할 권한이 없습니다.");
        }

        // 반환 대상 사용자를 담은 리스트 생성
        List<UserMember> users;

        if ("ROLE_MASTER".equals(currentUserRole)) {
            // 최고 관리자의 경우 모든 사용자 삽입
            users = userRepository.findAll();
        } else {
            // 중간 관리자의 경우 본인 회사에 속한 사용자만 삽입
            Company myCompany = companyRepository.findByCompanyId(currentCompanyId);
            users = userRepository.findByCompanyId(myCompany);
        }

        // DTO로 변환 후 반환
        return UserInfoResponseDTO.from(users);
    }

    /**
     * 사용자를 삭제합니다.
     *
     * @param targetEmployeeId 삭제할 대상의 사원번호.
     * @throws AccessDeniedException     현재 사용자에게 대상의 삭제 권한이 없는 경우 발생합니다.
     * @throws UsernameNotFoundException 대상을 잘못 지정한 경우 발생합니다.
     * @throws NullPointerException      세션이 유효하지 않은 경우 발생합니다.
     */
    @Transactional
    public void deleteUser(Long targetEmployeeId) throws AccessDeniedException, UsernameNotFoundException, NullPointerException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String[] parts = Objects.requireNonNull(auth).getName().split("_");
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        Long currentCompanyId = Long.parseLong(parts[0]);
        Long currentEmployeeId = Long.parseLong(parts[1]);

        UserMember targetUser = userRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new UsernameNotFoundException("유저가 존재하지 않습니다."));

        if ("ROLE_USER".equals(currentUserRole)) {
            throw new AccessDeniedException("하위관리자는 탈퇴 권한이 없습니다.");
        }

        if ("ROLE_MANAGER".equals(currentUserRole)) {
            if (!currentCompanyId.equals(targetUser.getCompanyId().getCompanyId())) {
                throw new AccessDeniedException("타 회사회원은 탈퇴시킬 수 없습니다.");
            }

            boolean isSelf = currentEmployeeId.equals(targetEmployeeId);
            boolean isTargetUserRole = targetUser.getRoleCode() == UserRole.ROLE_USER;

            if (!isSelf && !isTargetUserRole) {
                throw new AccessDeniedException("중간관리자는 본인 또는 하위관리자만 탈퇴 시킬 수 있습니다.");
            }
        }
        userRepository.delete(targetUser);
    }

    /**
     * 사용자의 등급을 수정합니다.
     *
     * @param targetEmployeeId 수정할 대상.
     * @param newRoleCode      새로 부여할 등급.
     * @throws AccessDeniedException     현재 사용자에게 수정 권한이 없는 경우 발생합니다.
     * @throws UsernameNotFoundException 대상이 유효하지 않은 경우 발생합니다.
     * @throws IllegalArgumentException  제공된 역할이 유효하지 않은 경우 발생합니다.
     * @throws NullPointerException      세션이 유효하지 않은 경우 발생합니다.
     */
    @Transactional
    public void updateRole(Long targetEmployeeId, String newRoleCode) throws AccessDeniedException, IllegalArgumentException, UsernameNotFoundException, NullPointerException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentRole = Objects.requireNonNull(auth).getAuthorities().iterator().next().getAuthority();

        if (!"ROLE_MASTER".equals(currentRole)) {
            throw new AccessDeniedException("권한 승격은 및 강등 진행은 최고관리자만 가능합니다.");
        }

        UserMember targetUser = userRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

        targetUser.setRoleCode(UserRole.valueOf(newRoleCode));
    }
}
