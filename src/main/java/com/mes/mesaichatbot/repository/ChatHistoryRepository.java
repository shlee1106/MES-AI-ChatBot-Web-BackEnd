package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.ChatHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatHistoryRepository extends CrudRepository<ChatHistory, Integer> {

}