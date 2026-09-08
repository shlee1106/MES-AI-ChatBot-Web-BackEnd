package com.mes.mesaichatbot.llm.validator;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.llm.dto.ChunkDefinition;
import com.mes.mesaichatbot.llm.dto.ChunkingResponse;
import com.mes.mesaichatbot.llm.dto.RowReference;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChunkingValidator {

    public void validate(
            List<UploadDataDetailsDto> rowData,
            ChunkingResponse response
    ) {

        if (rowData == null || rowData.isEmpty()) {
            throw new IllegalArgumentException(
                    "Chunking 대상 row가 없습니다."
            );
        }

        if (response == null
                || response.chunks() == null
                || response.chunks().isEmpty()) {

            throw new IllegalArgumentException(
                    "LLM Chunking 결과가 없습니다."
            );
        }

        /*
         * 원본 데이터의 (dataId, rowNumber)를 기준으로
         * 유효한 row인지 검증하기 위한 Map
         */
        Map<RowKey, Integer> originalOrder = new HashMap<>();

        for (int i = 0; i < rowData.size(); i++) {

            UploadDataDetailsDto row = rowData.get(i);

            RowKey key = new RowKey(
                    row.dataId(),
                    row.rowNumber()
            );

            if (originalOrder.containsKey(key)) {
                throw new IllegalArgumentException(
                        "중복된 row가 존재합니다: " + key
                );
            }

            originalOrder.put(key, i);
        }

        /*
         * LLM이 사용한 원본 row
         */
        Set<RowKey> usedRowData = new HashSet<>();

        /*
         * 전체 Chunk 순서 검증
         */
        int previousGlobalIndex = -1;

        /*
         * chunkId 중복 검증
         */
        Set<String> chunkIds = new HashSet<>();

        for (ChunkDefinition chunk : response.chunks()) {

            validateChunkId(chunk);

            if (!chunkIds.add(chunk.chunkId())) {
                throw new IllegalArgumentException(
                        "중복된 chunkId가 존재합니다: "
                                + chunk.chunkId()
                );
            }

            if (chunk.rows() == null
                    || chunk.rows().isEmpty()) {

                throw new IllegalArgumentException(
                        "Chunk에 row가 없습니다: "
                                + chunk.chunkId()
                );
            }

            /*
             * 현재 Chunk 내부 순서
             */
            int previousChunkIndex = -1;

            for (RowReference rowReference : chunk.rows()) {

                if (rowReference == null) {
                    throw new IllegalArgumentException(
                            "Chunk에 null row가 존재합니다: "
                                    + chunk.chunkId()
                    );
                }

                RowKey key = new RowKey(
                        rowReference.dataId(),
                        rowReference.rowNumber()
                );

                /*
                 * 존재하지 않는 원본 row인지 확인
                 */
                Integer originalIndex =
                        originalOrder.get(key);

                if (originalIndex == null) {
                    throw new IllegalArgumentException(
                            "LLM이 존재하지 않는 row를 반환했습니다: "
                                    + key
                    );
                }

                /*
                 * 여러 Chunk에서 같은 row를 사용했는지 확인
                 */
                if (!usedRowData.add(key)) {
                    throw new IllegalArgumentException(
                            "동일한 row가 여러 Chunk에 포함되었습니다: "
                                    + key
                    );
                }

                /*
                 * Chunk 내부 순서 검증
                 *
                 * 중요한 점:
                 * rowNumber 자체를 비교하는 것이 아니라
                 * 원본 List에서의 위치를 비교한다.
                 */
                if (originalIndex <= previousChunkIndex) {
                    throw new IllegalArgumentException(
                            "Chunk 내부 row 순서가 잘못되었습니다: "
                                    + chunk.chunkId()
                    );
                }

                previousChunkIndex = originalIndex;
            }

            /*
             * Chunk 전체 순서 검증
             *
             * 현재 Chunk의 첫 row가 이전 Chunk의 마지막 row보다
             * 뒤에 있어야 한다.
             */
            if (previousChunkIndex <= previousGlobalIndex) {
                throw new IllegalArgumentException(
                        "Chunk 순서가 잘못되었습니다: "
                                + chunk.chunkId()
                );
            }

            previousGlobalIndex = previousChunkIndex;
        }

        /*
         * 모든 원본 row가 사용되었는지 확인
         */
        if (usedRowData.size() != rowData.size()) {

            List<RowKey> missingrowData =
                    rowData.stream()
                            .map(row -> new RowKey(
                                    row.dataId(),
                                    row.rowNumber()
                            ))
                            .filter(key -> !usedRowData.contains(key))
                            .toList();

            throw new IllegalArgumentException(
                    "LLM 결과에서 누락된 row: "
                            + missingrowData
            );
        }
    }

    private void validateChunkId(ChunkDefinition chunk) {

        if (chunk.chunkId() == null
                || chunk.chunkId().isBlank()) {

            throw new IllegalArgumentException(
                    "chunkId가 비어 있습니다."
            );
        }
    }

    /**
     * 원본 row의 유일한 식별자
     */
    private record RowKey(
            int dataId,
            int rowNumber
    ) {
    }
}