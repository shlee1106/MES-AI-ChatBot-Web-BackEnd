package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHistoryNoSqlRepository extends JpaRepository<ChatHistoryNoSql, String> {
}
