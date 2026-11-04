package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.Company;
import org.springframework.data.repository.CrudRepository;

public interface CompanyRepository extends CrudRepository<Company, Long> {
    Company findByCompanyId(Long companyId);
}
