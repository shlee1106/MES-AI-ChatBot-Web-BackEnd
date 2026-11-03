package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.dto.JoinRequestDTO;
import com.mes.mesaichatbot.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입(사원 계정 생성) 관련 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JoinController {

    private final UserService userService;

    /**
     * 새로운 사원 계정을 생성합니다.
     *
     * @param dto 사용자 생성 요청 DTO ({@link JoinRequestDTO})
     * @return 생성 결과 메시지 및 상태 코드
     */
    @PostMapping(path = "/admin/users", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createUser(@Valid @RequestBody JoinRequestDTO dto) {
        try {
            // 사원 계정 생성 시도 (성공 시 201 Created 반환)
            userService.createUser(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("계정이 성공적으로 생성 완료.");
        } catch (AccessDeniedException e) {
            // 계정 생성 권한이 없거나 타 회사 사원을 생성하려 할 경우 403 Forbidden 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (DuplicateKeyException e) {
            // 이미 동일한 회사번호·사원번호로 등록된 계정이 있을 경우 409 Conflict 반환
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 잘못된 요청 인자일 경우 400 Bad Request 반환
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NullPointerException e) {
            // 인증 세션 정보가 유효하지 않을 경우 401 Unauthorized 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("세션이 유효하지 않습니다.");
        }
    }
}
