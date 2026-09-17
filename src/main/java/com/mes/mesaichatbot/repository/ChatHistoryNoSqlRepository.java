package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryNoSqlRepository extends CrudRepository<ChatHistoryNoSql, Integer> {

    List<ChatHistoryNoSql> findByCompanyIdAndEmployeeId(int companyId, int employeeId);
}