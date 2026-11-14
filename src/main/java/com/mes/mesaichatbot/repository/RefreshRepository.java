package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.RefreshEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RefreshRepository extends CrudRepository<RefreshEntity, String> {

    /**
     * 사용자 식별자("회사번호_사원번호")로 발급된 모든 Refresh Token을 조회합니다.
     */
    List<RefreshEntity> findAllByUsername(String username);

//    // 전달받은 refresh 토큰이 Redis에 존재하는지 확인
//    Boolean existsByRefresh(String refresh);
//
//    // 전달받은 refresh 토큰을 Redis에서 삭제
//    // JPA를 사용하여 삭제 쿼리를 날릴 때는 @Transactional 어노테이션이 필수.
//    @Transactional
//    void deleteByRefresh(String refresh);

}
