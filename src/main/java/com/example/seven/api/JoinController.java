package com.example.seven.api;

import com.example.seven.domain.user.dto.JoinRequestDTO; // [수정] DTO 변경
import com.example.seven.domain.user.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class JoinController {

    private final UserService userService;

    public JoinController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/join")
    public String join(@RequestBody JoinRequestDTO dto) { // [수정] 매개변수 타입 변경
        userService.join(dto);
        return "ok";
    }
}