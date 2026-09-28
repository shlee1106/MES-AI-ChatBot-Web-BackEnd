package com.mes.mesaichatbot.controller;

import com.mes.mesaichatbot.domain.user.service.UserService;
import com.mes.mesaichatbot.dto.UserMemberResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserMemberResponseDto> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo().toDto());
    }
}
