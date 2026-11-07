package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.Company;
import com.mes.mesaichatbot.domain.FileMetadata;
import com.mes.mesaichatbot.domain.UserMember;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileMetadataRepository extends CrudRepository<FileMetadata, String> {
    List<FileMetadata> findAllByEmployeeId(UserMember employeeId);

    @Query("SELECT f.fileId FROM FileMetadata f WHERE (f.employeeId.company = :companyId AND f.shared = true) OR" +
            "(f.employeeId.company = :companyId AND f.employeeId = :employeeId)")
    List<String> findAccessibleFileIds(@Param("companyId") Company company,
                                       @Param("employeeId") UserMember employeeId);
}
