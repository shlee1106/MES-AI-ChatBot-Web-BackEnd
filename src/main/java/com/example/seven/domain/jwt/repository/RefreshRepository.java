package com.example.seven.domain.jwt.repository;

import com.example.seven.domain.jwt.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface RefreshRepository extends CrudRepository<RefreshEntity, String> {

}
