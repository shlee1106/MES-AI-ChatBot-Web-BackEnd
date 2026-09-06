package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.CredentialNoSql;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialNoSqlRepository extends JpaRepository<CredentialNoSql, String> {
}
