package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.*;
import com.mes.mesaichatbot.dto.UserQuestionDto;
import com.mes.mesaichatbot.service.LlmChatService;
import com.mes.mesaichatbot.service.LlmChatSessionService;
import com.mes.mesaichatbot.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ChatController")
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {
    @Mock
    UserService userService;
    @Mock
    LlmChatSessionService sessionService;
    @Mock
    LlmChatService chatService;
    MockMvc mvc;
    ObjectMapper mapper = new ObjectMapper();
    Company company;
    UserMember member;
    ChatSession session;
    LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(userService, sessionService, chatService)).build();
        company = new Company(1L, "MES", now);
        member = new UserMember(2L, company, "Kim",
                                "pw", "k@example.com", UserRole.ROLE_USER, now);
        session = new ChatSession("session-1", member, "Help", now);
        when(userService.getCurrentUserEntity()).thenReturn(member);
    }

    @Test
    @DisplayName("질문 시 LLM의 답변 혹은 오류를 반환하는가?")
    void asksLlmWithMockedAnswerAndMapsBadRequestAndUnauthorized() throws Exception {
        when(chatService.askToLlm(eq("session-1"), eq(member), any(), any()))
                .thenReturn(
                        new ChatHistory("chat-1", member, session,
                                        "mocked answer", now, "assistant"));
        var body = mapper.writeValueAsString(new UserQuestionDto("질문"));
        mvc.perform(post("/api/v1/chat/sessions/session-1/ask")
                            .contentType("application/json").content(body))
           .andExpect(status().isOk()).andExpect(jsonPath("$.message")
                                                         .value("mocked answer"));
        when(chatService.askToLlm(eq("session-1"), eq(member), any(), any()))
                .thenThrow(new IllegalArgumentException());
        mvc.perform(post("/api/v1/chat/sessions/session-1/ask")
                            .contentType("application/json").content(body))
           .andExpect(status().isBadRequest()).andExpect(content().string(""));
        when(userService.getCurrentUserEntity()).thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(post("/api/v1/chat/sessions/session-1/ask")
                            .contentType("application/json").content(body))
           .andExpect(status().isUnauthorized()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("대화방 생성 시 결과 혹은 오류를 반환하는가?")
    void createsSessionAndMapsSessionErrors() throws Exception {
        when(sessionService.createChatSession(eq(member), any(), any())).thenReturn(session);
        mvc.perform(post("/api/v1/chat/sessionrooms").contentType("application/json")
                                                     .content("{\"title\":\"Help\"}"))
           .andExpect(status().isOk()).andExpect(jsonPath("$.sessionId").value("session-1"));
        when(sessionService.createChatSession(eq(member), any(), any())).thenThrow(new IllegalArgumentException());
        mvc.perform(post("/api/v1/chat/sessionrooms").contentType("application/json")
                                                     .content("{\"title\":\"Help\"}"))
           .andExpect(status().isBadRequest()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("대화 기록 접근 시 결과 혹은 오류를 반환하는가?")
    void readsAndDeletesHistoryAndSessions() throws Exception {
        when(sessionService.getChatHistories("session-1", member)).thenReturn(
                List.of(new ChatHistory("chat-1", member,
                                        session, "hello", now, "user")));
        mvc.perform(get("/api/v1/chat/sessions/session-1/histories")).andExpect(status().isOk())
           .andExpect(jsonPath("$[0].message").value("hello"));
        when(sessionService.getChatHistories("denied", member))
                .thenThrow(new AccessDeniedException("denied"));
        mvc.perform(get("/api/v1/chat/sessions/denied/histories")).andExpect(status().isForbidden())
           .andExpect(content().string(""));
        when(sessionService.getChatSessions(member)).thenReturn(List.of(session));
        mvc.perform(get("/api/v1/chat/sessions")).andExpect(status().isOk())
           .andExpect(jsonPath("$[0].sessionId").value("session-1"));
        assertThat(mvc.perform(delete("/api/v1/chat/sessions/session-1"))
                      .andExpect(status().isOk()).andReturn()
                      .getResponse().getContentAsString()).isNotEmpty();
        doThrow(new AccessDeniedException("denied")).when(sessionService)
                                                    .deleteChatSessions("denied", member);
        mvc.perform(delete("/api/v1/chat/sessions/denied")).andExpect(status().isForbidden())
           .andExpect(content().string("denied"));
        doThrow(new IllegalArgumentException("missing")).when(sessionService)
                                                        .deleteChatSessions("missing", member);
        mvc.perform(delete("/api/v1/chat/sessions/missing")).andExpect(status().isNotFound())
           .andExpect(content().string(""));
    }
}
