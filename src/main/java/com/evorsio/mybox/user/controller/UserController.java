package com.evorsio.mybox.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    @GetMapping("/hello")
    public String hello(Authentication authentication) {
        String username = authentication.getName();
        return "Hello, " + username + "! JWT验证成功。";
    }
}
