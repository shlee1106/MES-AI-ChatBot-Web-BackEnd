package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryNoSqlRepository extends MongoRepository<ChatHistoryNoSql, String> {

    // 회사 및 사원 ID가 검색에 필요한 추상메서드 선언
    @Query(value = "{ 'companyId': ?0, 'employeeId': ?1 }", sort = "{ 'recordedAt': 1 }")
    List<ChatHistoryNoSql> findByCompanyAndEmployee(String companyId, String employeeId);
}