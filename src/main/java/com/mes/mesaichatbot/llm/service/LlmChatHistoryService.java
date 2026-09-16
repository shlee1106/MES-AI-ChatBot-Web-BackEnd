package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import com.mes.mesaichatbot.repository.ChatHistoryNoSqlRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 대화 내역을 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class LlmChatHistoryService {

    //인터페이스 메소드 사용을 위한 객체생성
    private final ChatHistoryNoSqlRepository chatHistoryNoSqlRepository;

    // 대화를 가져오는지에 테스트용 메서드
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        System.out.println("=== 서버 시작: 대화 내역 조회 테스트 ===");
        List<ChatHistoryNoSql> history = getChatHistory("0", "0");
        System.out.println("조회된 건수: " + history.size());
    }

    public List<ChatHistoryNoSql> getChatHistory(String companyId,String employeeId) {
        // 대화 내역 불러오는 클래스
        List<ChatHistoryNoSql> historyList = chatHistoryNoSqlRepository.findByCompanyAndEmployee(companyId, employeeId);

        return historyList;
    }

    /**
     * 대화 내역을 UUID로 구분하여 저장하는 변수
     */
    //TODO 대화내역 회사회원 관계없이 다 저장하는데 필요한 부분인지 체크
    @Getter
    private final Map<UUID, List<Message>> chatHistories = new HashMap<>();
}
