package com.mes.mesaichatbot.llm.dto;

import java.util.List;

public record ChunkingResponse(
        List<ChunkDefinition> chunks
) {
}