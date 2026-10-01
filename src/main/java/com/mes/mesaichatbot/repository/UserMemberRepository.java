package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
//import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMemberRepository extends JpaRepository<UserMember, Long> {

    boolean existsByCompanyIdAndEmployeeId(Company companyId, Long employeeId);

    Optional<UserMember> findByCompanyIdAndEmployeeId(Company companyId, Long employeeId);

    Optional<UserMember> findByCompanyIdAndEmployeeIdAndCompanyEmail(Company companyId, Long employeeId, String companyEmail);

    List<UserMember> findByCompanyId(Company companyId);
}

