package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import org.springframework.data.repository.CrudRepository;

public interface ChatHistoryNoSqlRepository extends CrudRepository<ChatHistoryNoSql, String> {
}
