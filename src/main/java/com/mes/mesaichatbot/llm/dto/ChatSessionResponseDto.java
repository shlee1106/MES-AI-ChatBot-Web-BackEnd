package com.mes.mesaichatbot.llm.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatSessionResponseDto {
    private String sessionId;
    private int companyId;
    private int employeeId;
    private String sessionTitle;
    private LocalDateTime createdAt;
}
