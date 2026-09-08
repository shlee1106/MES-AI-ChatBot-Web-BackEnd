package com.mes.mesaichatbot.llm.dto;

import java.util.List;

/**
 * 각 청크의 형식을 지정합니다.
 *
 * @param chunkId {@code chunk-***} 형식의 청크 ID.
 * @param rows    각 블록의 정보.
 */
public record ChunkDefinition(
        String chunkId,
        List<RowReference> rows
) {
}