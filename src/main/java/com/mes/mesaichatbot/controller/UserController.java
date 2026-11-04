package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.dto.JoinRequestDTO;
import com.mes.mesaichatbot.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 관련 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 내 정보를 조회합니다.
     *
     * @return 내 정보.
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDTO> getMyInfo() {
        try {
            // 내 정보 조회.
            return ResponseEntity.ok(userService.getMyInfo());
        } catch (NullPointerException | UsernameNotFoundException e) {
            // 인증되지 않은 사용자인 경우 401 Unauthorized 응답 반환.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 관리자용 사용자 정보 조회.
     *
     * @return 사용자 정보 리스트.
     */
    @GetMapping
    public ResponseEntity<List<UserInfoResponseDTO>> getUserForAdmin() {
        try {
            return ResponseEntity.ok(userService.getUserForAdmin());
        } catch (AccessDeniedException e) {
            // 조회 권한이 없는 일반 사원(ROLE_USER)인 경우 403 Forbidden 응답 반환.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NullPointerException e) {
            // 세션이 유효하지 않은 경우 401 응답 반환.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 새로운 사원 계정을 생성합니다.
     *
     * @param dto 사용자 생성 요청 DTO ({@link JoinRequestDTO})
     * @return 생성 결과 메시지 및 상태 코드
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
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

    /**
     * 사용자를 삭제합니다.
     *
     * @param targetEmployeeId 삭제 대상 사용자 ID
     * @return 응답 메시지.
     */
    @DeleteMapping("/{target}")
    public ResponseEntity<String> deleteUser(@PathVariable("target") Long targetEmployeeId) {
        try {
            // 사용자 삭제
            userService.deleteUser(targetEmployeeId);
            return ResponseEntity.ok("회원이 탈퇴되었습니다.");
        } catch (AccessDeniedException e) {
            // 권한이 없는 경우 403 응답 반환.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (UsernameNotFoundException e) {
            // 사용자를 찾을 수 없는 경우 404 응답 반환.
            return ResponseEntity.notFound().build();
        } catch (NullPointerException e) {
            // 세션이 유효하지 않은 경우 401 응답 반환.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("세션이 유효하지 않습니다.");
        }
    }

    /**
     * 사용자의 등급을 수정합니다.
     *
     * @param targetEmployeeId 수정 대상 사용자 ID
     * @param newRoleCode      새로운 등급 코드
     * @return 응답 메시지.
     */
    @PatchMapping("/{target}")
    public ResponseEntity<String> updateRole(@PathVariable("target") Long targetEmployeeId,
                                             @RequestBody String newRoleCode) {
        try {
            // 등급 수정
            userService.updateRole(targetEmployeeId, newRoleCode);
            return ResponseEntity.ok("권한이 수정되었습니다.");
        } catch (AccessDeniedException e) {
            // 권한이 없는 경우 403 응답 반환.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (UsernameNotFoundException e) {
            // 대상이 존재하지 않는 경우 404 응답 반환.
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            // 등급이 유효하지 않은 경우 400 응답 반환.
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NullPointerException e) {
            // 세션이 유효하지 않은 경우 401 응답 반환.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("세션이 유효하지 않습니다.");
        }
    }
}
