package com.mes.mesaichatbot.llm.service;

import lombok.Getter;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 대화 내역을 관리합니다.
 */
@Service
public class LlmChatHistoryService {
    public LlmChatHistoryService() {
        // TODO: 시작 시 DB에서 대화 내역 불러오기
    }

    /**
     * 대화 내역을 UUID로 구분하여 저장하는 변수
     */
    @Getter
    private final Map<UUID, List<Message>> chatHistories = new HashMap<>();
}
