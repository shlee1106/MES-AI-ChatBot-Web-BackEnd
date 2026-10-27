package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FileMetadataRepository extends CrudRepository<FileMetadata, String> {
    List<FileMetadata> findAllByEmployeeId(UserMember employeeId);
}
