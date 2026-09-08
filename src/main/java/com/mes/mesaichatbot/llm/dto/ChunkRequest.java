package com.mes.mesaichatbot.llm.dto;

import java.util.List;

/**
 * LLM에 전송할 데이터 규격을 정의합니다.
 *
 * @param document       문서의 메타데이터를 정의합니다.
 * @param chunkingPolicy 청킹 설정을 정의합니다.
 * @param rows           실제 데이터를 여기에 입력합니다.
 */
public record ChunkRequest(DocumentData document, ChunkingPolicy chunkingPolicy, List<SimplifiedData> rows) {
}