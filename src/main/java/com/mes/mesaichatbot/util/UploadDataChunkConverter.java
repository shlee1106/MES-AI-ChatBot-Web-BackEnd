package com.mes.mesaichatbot.util;

import com.mes.mesaichatbot.dto.ChunkingResponse;
import com.mes.mesaichatbot.dto.RowReference;
import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 업로드된 문서를 변환하기 위한 도구의 모음.
 */
@Slf4j
@Component
public class UploadDataChunkConverter {

    private final ChunkingValidator validator;

    public UploadDataChunkConverter(ChunkingValidator validator) {
        this.validator = validator;
    }

    /**
     * 각 행의 정보를 청크 데이터와 매핑하여
     * {@code List<Document>} 형태로 변환합니다.
     *
     * @param rows     {@code List<UploadDataDetailsDto>}
     * @param response LLM이 반환한 청크 데이터.
     * @return {@code List<UploadDataDetailsDto>}
     * @throws IllegalArgumentException 청크 데이터가 잘못됨.
     */
    public List<Document> convert(
            List<UploadDataDetailsDto> rows,
            ChunkingResponse response
    ) throws IllegalArgumentException {

        // LLM 답변의 유효성 검사
        validator.validate(rows, response);

        // 각 UploadDetailsDto에 행 참조 데이터 부여
        Map<RowReference, UploadDataDetailsDto> rowMap =
                rows.stream()
                    .collect(Collectors.toMap(
                            row -> new RowReference(
                                    row.dataId(),
                                    row.rowNumber()
                            ),
                            Function.identity()
                    ));

        // 각 청크 데이터를 List<Document> 에 매핑
        return response.chunks()
                       .stream()
                       .map(chunk -> {
                           String content = chunk.rows().stream().map(ref -> rowMap.get(new RowReference(
                                   ref.dataId(),
                                   ref.rowNumber()
                           )).rowData()).collect(Collectors.joining("\n\n"));

                           Map<String, Object> metadata =
                                   new HashMap<>();

                           UploadDataDetailsDto firstRow =
                                   rowMap.get(chunk.rows().getFirst());

                           metadata.put(
                                   "companyId",
                                   firstRow.companyId()
                           );

                           metadata.put(
                                   "fileId",
                                   firstRow.fileId()
                           );

                           metadata.put(
                                   "chunkId",
                                   chunk.chunkId()
                           );

                           metadata.put(
                                   "rowNumbers",
                                   chunk.rows()
                           );

                           metadata.put(
                                   "startRowNumber",
                                   chunk.rows().getFirst()
                           );

                           metadata.put(
                                   "endRowNumber",
                                   chunk.rows().getLast()
                           );

                           return new Document(
                                   String.format("doc-%s-%s", rows.getFirst().fileId(), chunk.chunkId()),
                                   content,
                                   metadata
                           );
                       })
                       .toList();
    }
}