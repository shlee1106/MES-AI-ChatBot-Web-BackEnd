package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatHistory;
import com.mes.mesaichatbot.domain.ChatSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends CrudRepository<ChatHistory, Integer> {

    List<ChatHistory> findAllBySessionIdOrderByRecordedAtAsc(ChatSession sessionId);
}