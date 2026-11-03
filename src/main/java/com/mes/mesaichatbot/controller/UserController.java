package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @GetMapping("/")
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
    public ResponseEntity<String> updateRole(@PathVariable("target") Long targetEmployeeId, @RequestBody String newRoleCode) {
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
