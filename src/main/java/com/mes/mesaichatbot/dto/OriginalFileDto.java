package com.mes.mesaichatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
/*
  대화 반환 형식에 사용될 Dto.
 */
public class OriginalFileDto {
    private String fileId;
    private int companyId;
    private int employeeId;
    private String originName;
    private String saveFileName;
    private String fileType;
    private String fileDataPath;
    private LocalDateTime uploadedAt;
}
