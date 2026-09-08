package com.mes.mesaichatbot.llm.converter;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import com.mes.mesaichatbot.llm.dto.ChunkingResponse;
import com.mes.mesaichatbot.llm.dto.RowReference;
import com.mes.mesaichatbot.llm.validator.ChunkingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UploadDataChunkConverter {

    private final ChunkingValidator validator;

    public UploadDataChunkConverter(ChunkingValidator validator) {
        this.validator = validator;
    }

    public List<Document> convert(
            List<UploadDataDetailsDto> rows,
            ChunkingResponse response
    ) {

        try {
            validator.validate(rows, response);
        } catch (IllegalArgumentException e) {
            log.error("데이터를 검증할 수 없습니다: {}", e.getMessage());
        }


        Map<RowReference, UploadDataDetailsDto> rowMap =
                rows.stream()
                        .collect(Collectors.toMap(
                                row -> new RowReference(
                                        row.dataId(),
                                        row.rowNumber()
                                ),
                                Function.identity()
                        ));

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