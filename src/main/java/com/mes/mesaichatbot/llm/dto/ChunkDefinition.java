package com.mes.mesaichatbot.llm.dto;

import java.util.List;

public record ChunkDefinition(
        String chunkId,
        List<RowReference> rows
) {
}