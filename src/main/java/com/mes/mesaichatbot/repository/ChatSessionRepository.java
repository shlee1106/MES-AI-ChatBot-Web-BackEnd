package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatSession;
import com.mes.mesaichatbot.domain.UserMember;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends CrudRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);

    List<ChatSession> findAllByEmployeeId(UserMember employeeId);
}
