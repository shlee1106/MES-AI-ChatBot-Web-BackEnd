package com.mes.mesaichatbot.util;

import com.mes.mesaichatbot.dto.ChunkDefinition;
import com.mes.mesaichatbot.dto.ChunkingResponse;
import com.mes.mesaichatbot.dto.RowReference;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LLM의 답변을 검증하는 도구.
 */
@Component
public class ChunkingValidator {

    /**
     * LLM이 반환한 데이터와 원본 데이터를 비교하여,
     * LLM의 데이터가 정확한지 확인합니다.
     *
     * @param rowData  원본 데이터.
     * @param response LLM의 답변.
     * @throws IllegalArgumentException LLM의 답변이 잘못됨.
     */
    public void validate(List<UploadDataDetailsDto> rowData,
                         ChunkingResponse response) throws IllegalArgumentException {

        // 원본 데이터가 없음
        if (rowData == null || rowData.isEmpty()) {
            throw new IllegalArgumentException("Chunking 대상 row가 없습니다.");
        }

        // LLM의 답변이 없음
        if (response == null || response.chunks() == null || response.chunks().isEmpty()) {

            throw new IllegalArgumentException("LLM Chunking 결과가 없습니다.");
        }

        /*
         * 원본 데이터의 (dataId, rowNumber)를 기준으로
         * 유효한 row인지 검증하기 위한 Map
         */
        Map<RowKey, Integer> originalOrder = new HashMap<>();

        // 원본 데이터 순회
        for (int i = 0; i < rowData.size(); i++) {
            // 각 데이터 가져오기
            UploadDataDetailsDto row = rowData.get(i);

            // 구분을 위해 객체 생성.
            RowKey key = new RowKey(row.dataId(), row.rowNumber());

            // 목록에 중복된 데이터가 있으면 예외 투척.
            if (originalOrder.containsKey(key)) {
                throw new IllegalArgumentException("중복된 row가 존재합니다: " + key);
            }

            // 중복이 없으면 목록에 데이터 추가.
            originalOrder.put(key, i);
        }

        // LLM이 사용한 원본 행을 담을 집합
        Set<RowKey> usedRowData = new HashSet<>();

        // 전체 청크 순서 검증을 위한 인덱스
        int previousGlobalIndex = -1;

        // chunkId 중복 검증용 집합
        Set<String> chunkIds = new HashSet<>();

        // 각 청크 순회
        for (ChunkDefinition chunk : response.chunks()) {
            // 청크 ID 존재 여부 확인
            validateChunkId(chunk);

            // 중복 검사
            // (집합의 add 함수는 중복이 발생하여
            //  원소가 추가되지 않은 경우,
            //  false를 반환함)
            if (!chunkIds.add(chunk.chunkId())) {
                throw new IllegalArgumentException("중복된 chunkId가 존재합니다: " + chunk.chunkId());
            }

            // chunk의 row 데이터 검사
            if (chunk.rows() == null || chunk.rows().isEmpty()) {

                throw new IllegalArgumentException("Chunk에 row가 없습니다: " + chunk.chunkId());
            }

            // 현재 청크의 내부 순서
            int previousChunkIndex = -1;

            // 각 청크의 행 순회
            for (RowReference rowReference : chunk.rows()) {
                // rowReference 객체가 비어 있을 때
                if (rowReference == null) {
                    throw new IllegalArgumentException("Chunk에 null row가 존재합니다: " + chunk.chunkId());
                }

                // 청크의 각 행의 데이터 조립
                RowKey key = new RowKey(rowReference.dataId(), rowReference.rowNumber());

                // 원본 행에서 인덱스를 통해 검색,
                // 없으면 예외 투척
                Integer originalIndex = originalOrder.get(key);

                if (originalIndex == null) {
                    throw new IllegalArgumentException("LLM이 존재하지 않는 row를 반환했습니다: " + key);
                }

                // row 중복 여부 확인
                if (!usedRowData.add(key)) {
                    throw new IllegalArgumentException("동일한 row가 여러 Chunk에 포함되었습니다: " + key);
                }

                // 청크의 순서가 원본 데이터의 순서와 일치하는지 검사
                if (originalIndex <= previousChunkIndex) {
                    throw new IllegalArgumentException("Chunk 내부 row 순서가 잘못되었습니다: " + chunk.chunkId());
                }

                // 현재 인덱스를 이전 인덱스에 대입 후 반복
                previousChunkIndex = originalIndex;
            }

            /*
             * 청크의 전체 순서 검증.
             * previousChunkIndex는 previousGlobalIndex보다 항상 커야 함.
             */
            if (previousChunkIndex <= previousGlobalIndex) {
                throw new IllegalArgumentException("Chunk 순서가 잘못되었습니다: " + chunk.chunkId());
            }

            previousGlobalIndex = previousChunkIndex;
        }

        // 모든 원본 row가 존재하는지 확인
        if (usedRowData.size() != rowData.size()) {
            // rowData 순회, 각 rowData를 RowKey에 매핑 후 누락된 행을 모음
            List<RowKey> missingrowData = rowData.stream().map(row -> new RowKey(row.dataId(), row.rowNumber()))
                                                 .filter(key -> !usedRowData.contains(key)).toList();

            // 행의 모음을 담아서 예외 투척
            throw new IllegalArgumentException("LLM 결과에서 누락된 row: " + missingrowData);
        }
    }

    /**
     * 각 청크 ID가 비어 있는지 검사합니다.
     *
     * @param chunk 청크 ID.
     */
    private void validateChunkId(ChunkDefinition chunk) {
        if (chunk.chunkId() == null || chunk.chunkId().isBlank()) {

            throw new IllegalArgumentException("chunkId가 비어 있습니다.");
        }
    }

    /**
     * 원본 행을 식별하기 위한 데이터 모음.
     */
    private record RowKey(int dataId, int rowNumber) {
    }
}