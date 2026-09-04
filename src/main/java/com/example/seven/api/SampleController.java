package com.example.seven.api;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SampleController {

    @GetMapping("/")
    public String index() {

        return "index";
    }

    @GetMapping("/user")
    public String user() {
        System.out.println(SecurityContextHolder.getContext().getAuthentication().getName());
        return "user";
    }

    @GetMapping("admin")
    public String admin() {

        return "admin";
    }
}
