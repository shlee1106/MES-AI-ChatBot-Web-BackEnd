package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.dto.JoinRequestDTO; // [수정] DTO 변경
import com.mes.mesaichatbot.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JoinController {

    private final UserService userService;

    @PostMapping("/admin/users")
    public ResponseEntity<String> createUser(@Valid @RequestBody JoinRequestDTO dto) { // [수정] 매개변수 타입 변경
        try {
            userService.createUser(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("계정이 성공적으로 생성 완료.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}