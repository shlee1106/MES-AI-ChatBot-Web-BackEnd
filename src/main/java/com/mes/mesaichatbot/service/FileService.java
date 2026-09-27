package com.mes.mesaichatbot.service;

import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;

    @Transactional
    public void delete(String fileId) {
        if (!fileMetadataRepository.existsById(fileId)) {
            throw new IllegalArgumentException("존재하지 않는 파일 ID입니다: " + fileId);
        }
        fileMetadataRepository.deleteById(fileId);
        log.info("MySQL 파일 메타데이터 삭제 완료: fileId = {}", fileId);
    }
}