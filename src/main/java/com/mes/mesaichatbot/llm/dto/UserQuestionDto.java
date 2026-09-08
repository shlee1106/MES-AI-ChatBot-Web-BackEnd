package com.mes.mesaichatbot.llm.dto;

/**
 * 사용자의 질문이 담길 객체.
 *
 * @param message 사용자의 질문.
 */
public record UserQuestionDto(String message) {
}
