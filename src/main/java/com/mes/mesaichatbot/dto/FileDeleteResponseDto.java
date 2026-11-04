package com.mes.mesaichatbot.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FileDeleteResponseDto {
    private String fileId;
    private String message;
    private LocalDateTime deletedAt;

    public static FileDeleteResponseDto of(String fileId) {
        return new FileDeleteResponseDto(
                fileId,
                "파일 메타데이터가 성공적으로 삭제되었습니다.",
                LocalDateTime.now()
        );
    }
}