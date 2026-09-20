package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.FileMetadata;
import org.springframework.data.repository.CrudRepository;

public interface FileMetadataRepository extends CrudRepository<FileMetadata, String> {
}
