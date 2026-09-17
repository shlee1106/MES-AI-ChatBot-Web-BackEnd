package com.mes.mesaichatbot.llm.service;

import com.mes.mesaichatbot.domain.ChatHistoryNoSql;
import com.mes.mesaichatbot.repository.ChatHistoryNoSqlRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 대화 내역을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatHistoryService {

    //인터페이스 메소드 사용을 위한 객체생성
    private final ChatHistoryNoSqlRepository chatHistoryNoSqlRepository;
    /**
     * 대화 내역을 UUID로 구분하여 저장하는 변수
     */
    //TODO 대화내역 회사회원 관계없이 다 저장하는데 필요한 부분인지 체크
    @Getter
    public final HashMap<UUID, ArrayList<Message>> chatHistories = new HashMap<>();

    /*
    서버 시작 시 자동 실행되는 메서드
    DB에서 대화 내역들을 꺼내서 chatHistories 맵에 할당
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // DB에서 대화 내역을 전부 꺼냄
        Iterable<ChatHistoryNoSql> rows = chatHistoryNoSqlRepository.findAll();
            // 각 행 순회
            for (ChatHistoryNoSql chatHistoryNoSql : rows) {
                    // 각 행의 대화방 ID UUID로 파싱
                    UUID chatId = UUID.fromString(chatHistoryNoSql.getChatroomId());
                    // 메시지를 담을 공간 생성
                    Message message;

                    // 대화 주체가 사용자면
                    if (chatHistoryNoSql.getRole().equals("user")) {
                        // UserMessage 형태로 생성 후 대입
                        message = UserMessage.builder().text(chatHistoryNoSql.getMessage()).build();
                    } else {
                        // 아니면 AssistantMessage 형태로 생성 후 대입
                        message = AssistantMessage.builder().content(chatHistoryNoSql.getMessage()).build();
                    }

                    // 해당 대화방이 전에 삽입되었다면
                    if (chatHistories.containsKey(chatId)) {
                        // 해당 대화방에 삽입
                        chatHistories.get(chatId).add(message);
                    } else {
                        // 아니면 새로 리스트 만들어서 삽입
                        ArrayList<Message> list = new ArrayList<>();
                        list.add(message);
                        chatHistories.put(chatId, list);
                    }
            }
    }
}
