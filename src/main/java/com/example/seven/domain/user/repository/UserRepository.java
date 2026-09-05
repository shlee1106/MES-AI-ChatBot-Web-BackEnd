package com.example.seven.domain.user.repository;

import com.example.seven.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 회사번호와 사원번호로 사용자 찾기 (로그인 시 사용)
    Optional<UserEntity> findByCompanyIdAndEmployeeId(Long companyId, Long employeeId);

    // 중복 가입 방지 체크용
    boolean existsByCompanyIdAndEmployeeId(Long companyId, Long employeeId);
}
