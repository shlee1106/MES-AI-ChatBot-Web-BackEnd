package com.mes.mesaichatbot.llm.dto;

/**
 * 하나의 블록 단위입니다.
 * dataId와 rowNumber로 문서의 영역을 식별합니다.
 *
 * @param dataId
 * @param rowNumber
 */
public record RowReference(int dataId,
                           int rowNumber) {
}
