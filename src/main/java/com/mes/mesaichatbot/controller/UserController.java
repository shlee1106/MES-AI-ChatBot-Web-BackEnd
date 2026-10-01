package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.domain.user.service.UserService;
import com.mes.mesaichatbot.dto.UserMemberResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponseDTO> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo());
    }

    @GetMapping("/")
    public ResponseEntity<List<UserInfoResponseDTO>> getUserForAdmin() {
        return ResponseEntity.ok(userService.getUserForAdmin());
    }

    @DeleteMapping("/{target}")
    public ResponseEntity<String> deleteUser(@PathVariable("target") Long targetEmployeeId) {
        userService.deleteUser(targetEmployeeId);
        return ResponseEntity.ok("회원이 탈퇴되었습니다.");
    }

    @PatchMapping("/{target}")
    public ResponseEntity<String> updateRole(@PathVariable("target") Long targetEmployeeId, String newRoleCode) {
        userService.updateRole(targetEmployeeId, newRoleCode);
        return ResponseEntity.ok("권한이 수정되었습니다.");
    }
}
