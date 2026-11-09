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

    @Query("SELECT f FROM FileMetadata f WHERE f.employeeId.company = :companyId AND f.shared = true ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findSharedFilesByCompany(@Param("companyId") Company company);

    @Query("SELECT f FROM FileMetadata f WHERE f.employeeId.company = :companyId ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findAllFilesByCompany(@Param("companyId") Company company);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FileMetadata f " +
            "WHERE f.employeeId.company = :companyId AND f.shared = true AND LOWER(f.originName) = LOWER(:originName)")
    boolean existsSharedByCompanyAndOriginName(@Param("companyId") Company company,
                                               @Param("originName") String originName);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FileMetadata f " +
            "WHERE f.employeeId = :employeeId AND f.shared = false AND LOWER(f.originName) = LOWER(:originName)")
    boolean existsPersonalByEmployeeAndOriginName(@Param("employeeId") UserMember employeeId,
                                                  @Param("originName") String originName);

    @Query("SELECT f.fileId FROM FileMetadata f WHERE (f.employeeId.company = :companyId AND f.shared = true) OR" +
            "(f.employeeId.company = :companyId AND f.employeeId = :employeeId)")
    List<String> findAccessibleFileIds(@Param("companyId") Company company,
                                       @Param("employeeId") UserMember employeeId);
}
