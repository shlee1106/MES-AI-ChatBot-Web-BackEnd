package com.mes.mesaichatbot.domain.user.repository;

import com.mes.mesaichatbot.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 회사번호와 사원번호로 사용자 찾기 (로그인 시 사용)
    Optional<UserEntity> findByCompanyIdAndEmployeeId(Long companyId, Long employeeId);

    // 비밀번호 수정 시 회사번호, 사원번호, 이메일로 사용자 찾기
    Optional<UserEntity> findByCompanyIdAndEmployeeIdAndEmail(Long companyId, Long employeeId, String email);

    // 중복 가입 방지 체크용
    boolean existsByCompanyIdAndEmployeeId(Long companyId, Long employeeId);
}
