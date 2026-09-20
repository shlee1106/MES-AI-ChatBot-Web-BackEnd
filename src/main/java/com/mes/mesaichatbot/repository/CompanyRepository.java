package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.Company;
import org.springframework.data.repository.CrudRepository;

public interface CompanyRepository extends CrudRepository<Company, Integer> {
    Company findByCompanyId(int companyId);
}
