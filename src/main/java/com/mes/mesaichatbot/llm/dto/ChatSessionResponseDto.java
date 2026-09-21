package com.mes.mesaichatbot.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatSessionResponseDto {
    private String sessionId;
    private Long companyId;
    private Long employeeId;
    private String sessionTitle;
    private LocalDateTime createdAt;
}
