package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.dto.JoinRequestDTO;
import com.mes.mesaichatbot.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        // 내 정보 조회.
        return ResponseEntity.ok(userService.getMyInfo());
    }

    /**
     * 관리자용 사용자 정보 조회.
     *
     * @return 사용자 정보 리스트.
     */
    @GetMapping
    public ResponseEntity<List<UserInfoResponseDTO>> getUserForAdmin() {
        // GlobalExceptionHandler파일안에 예외처리 권한 넘김
        // AccessDeniedException: 조회 권한이 없는 일반 사원인 경우 403 Forbidden 반환
        // AuthenticationCredentialsNotFoundException: 인증 세션이 유효하지 않은 경우 401 Unauthorized 반환
        return ResponseEntity.ok(userService.getUserForAdmin());
    }

    /**
     * 새로운 사원 계정을 생성합니다.
     *
     * @param dto 사용자 생성 요청 DTO ({@link JoinRequestDTO})
     * @return 생성 결과 메시지 및 상태 코드
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createUser(@Valid @RequestBody JoinRequestDTO dto) {
        // GlobalExceptionHandler파일안에 예외처리 권한 넘김
        // MethodArgumentNotValidException: @Valid 필수값/형식 검증 실패 시 400 Bad Request 반환
        // AccessDeniedException: 생성 권한이 없거나 타 회사 사원 생성 시도 시 403 Forbidden 반환
        // DuplicateKeyException: 동일한 회사번호,사원번호 계정이 이미 존재할 경우 409 Conflict 반환
        // IllegalArgumentException: 잘못된 요청 인자일 경우 400 Bad Request 반환
        // AuthenticationCredentialsNotFoundException: 인증정보가 유효하지 않을 경우 401 Unauthorized 반환
        userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("계정이 성공적으로 생성 완료.");
    }

    /**
     * 사용자를 삭제합니다.
     *
     * @param targetEmployeeId 삭제 대상 사용자 ID
     * @return 응답 메시지.
     */
    @DeleteMapping("/{target}")
    public ResponseEntity<String> deleteUser(@PathVariable("target") Long targetEmployeeId) {
        // GlobalExceptionHandler파일안에 예외처리 권한 넘김
        // - AccessDeniedException: 삭제 권한이 없는 경우 403 Forbidden 반환
        // - UsernameNotFoundException: 삭제 대상 사용자를 찾을 수 없는 경우 404 Not Found 반환
        // - AuthenticationCredentialsNotFoundException: 인증이 유효하지 않은 경우 401 Unauthorized 반환
        userService.deleteUser(targetEmployeeId);
        return ResponseEntity.ok("회원이 탈퇴되었습니다.");
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
        // GlobalExceptionHandler파일안에 예외처리 권한 넘김
        // AccessDeniedException: 등급 수정 권한이 없는 경우 403 Forbidden 반환
        // UsernameNotFoundException: 수정 대상 사용자를 찾을 수 없는 경우 404 Not Found 반환
        // IllegalArgumentException: 요청한 등급 코드가 유효하지 않은 경우 400 Bad Request 반환
        // AuthenticationCredentialsNotFoundException: 인증이 유효하지 않은 경우 401 Unauthorized 반환
        userService.updateRole(targetEmployeeId, newRoleCode);
        return ResponseEntity.ok("권한이 수정되었습니다.");
    }
}
