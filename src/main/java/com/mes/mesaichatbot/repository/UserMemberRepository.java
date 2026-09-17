package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.UserMember;
import org.springframework.data.repository.CrudRepository;

public interface UserMemberRepository extends CrudRepository<UserMember, Integer> {
    UserMember findByCompanyId(int companyId);
}
