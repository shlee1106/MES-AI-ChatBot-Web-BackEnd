package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.UserMember;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserMemberRepository extends CrudRepository<UserMember, Long> {

    boolean existsByCompanyIdAndEmployeeId(Company companyId, Long employeeId);

    Optional<UserMember> findByCompanyIdAndEmployeeId(Company companyId, Long employeeId);

    Optional<UserMember> findByCompanyIdAndEmployeeIdAndCompanyEmail(Company companyId, Long employeeId, String companyEmail);
}

