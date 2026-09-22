package com.mes.mesaichatbot.controller;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SampleController {

    @GetMapping("/")
    public String index() {
        return "기본 페이지(누구나 접근 가능)";
    }

    @GetMapping("/user")
    public String user() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return "접속자 : " + username + " - 일반사원(USER)이상 접근 가능한 페이지.";
    }

    @GetMapping("/manager")
    public String manager() {
        return "중간관리자(MANAGER) 이상 접근 가능한 페이지.";
    }

    @GetMapping("/admin")
    public String admin() {
        return "최고관리자(MASTER) 전용 페이지.";
    }
}
