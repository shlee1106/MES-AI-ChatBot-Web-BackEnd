package com.mes.mesaichatbot.llm.dto;

/**
 * 청킹 설정을 정의합니다.
 *
 * @param targetTokens 각 청크의 목표 토큰 수입니다.
 * @param maxTokens    각 청크의 최대 토큰 수입니다.
 * @param minTokens    각 청크의 최소 토큰 수입니다.
 */
public record ChunkingPolicy(int targetTokens, int maxTokens, int minTokens) {
    public ChunkingPolicy() {
        this(500, 800, 100);
    }
}
