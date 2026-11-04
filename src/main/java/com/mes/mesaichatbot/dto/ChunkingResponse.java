package com.mes.mesaichatbot.dto;

import java.util.List;

/**
 * 문서 청킹 과정에서 LLM이 반환할 데이터의 형식을 정의합니다.
 *
 * @param chunks
 */
public record ChunkingResponse(
        List<ChunkDefinition> chunks
) {
}