package com.mes.mesaichatbot.llm.dto;

/**
 * 사용자의 대화 세션 생성 요청의 형식을 정의합니다.
 *
 * @param title 대화방의 이름
 */
public record ChatSessionRequestDto(String title) {
}
