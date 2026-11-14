package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import com.mes.mesaichatbot.domain.UserMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사원(UserMember) 엔티티 조회 및 관리를 담당하는 JPA 리포지토리.
 * 복합키({@link UserMemberId}) 구조에 맞춰 JPQL 쿼리를 매핑합니다.
 */
public interface UserMemberRepository extends JpaRepository<UserMember, UserMemberId> {

    /**
     * 회사 엔티티와 사원번호(복합키 내부 필드)로 사원 존재 여부를 확인합니다.
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserMember u WHERE u.company = :companyId AND u.id.employeeId = :employeeId")
    boolean existsByCompanyIdAndEmployeeId(@Param("companyId") Company companyId, @Param("employeeId") Long employeeId);

    /**
     * 회사 엔티티와 사원번호(복합키 내부 필드)로 사원 정보를 조회합니다.
     */
    @Query("SELECT u FROM UserMember u WHERE u.company = :companyId AND u.id.employeeId = :employeeId AND u.deleted = false")
    Optional<UserMember> findByCompanyIdAndEmployeeId(@Param("companyId") Company companyId, @Param("employeeId") Long employeeId);

    /**
     * 회사 엔티티, 사원번호, 이메일이 모두 일치하는 사원 정보를 조회합니다.
     */
    @Query("SELECT u FROM UserMember u WHERE u.company = :companyId AND u.id.employeeId = :employeeId AND u.companyEmail = :companyEmail AND u.deleted = false")
    Optional<UserMember> findByCompanyIdAndEmployeeIdAndCompanyEmail(@Param("companyId") Company companyId, @Param("employeeId") Long employeeId, @Param("companyEmail") String companyEmail);

    /**
     * 특정 회사에 소속된 모든 사원 목록을 조회합니다.
     */
    @Query("SELECT u FROM UserMember u WHERE u.company = :companyId AND u.deleted = false")
    List<UserMember> findByCompanyId(@Param("companyId") Company companyId);

    /**
     * 탈퇴하지 않은 모든 사원 목록을 조회합니다.
     */
    List<UserMember> findAllByDeletedFalse();
}
