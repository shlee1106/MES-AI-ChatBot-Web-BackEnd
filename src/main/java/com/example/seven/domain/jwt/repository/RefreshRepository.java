package com.example.seven.domain.jwt.repository;

import com.example.seven.domain.jwt.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshRepository extends CrudRepository<RefreshEntity, String> {

//    // 전달받은 refresh 토큰이 Redis에 존재하는지 확인
//    Boolean existsByRefresh(String refresh);
//
//    // 전달받은 refresh 토큰을 Redis에서 삭제
//    // JPA를 사용하여 삭제 쿼리를 날릴 때는 @Transactional 어노테이션이 필수.
//    @Transactional
//    void deleteByRefresh(String refresh);

}
