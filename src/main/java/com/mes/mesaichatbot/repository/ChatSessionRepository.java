package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatSession;
import org.springframework.data.repository.CrudRepository;

public interface ChatSessionRepository extends CrudRepository<ChatSession, Integer> {
}
