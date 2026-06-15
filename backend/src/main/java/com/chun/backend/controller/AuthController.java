package com.chun.backend.controller;

import com.chun.backend.dto.AuthRequest;
import com.chun.backend.model.User;
import com.chun.backend.security.JwtUtil;
import com.chun.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // 注入 UserService、JwtUtil
    private final UserService userService;
    private  final JwtUtil jwtUtil;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody AuthRequest authRequest) {
        // 呼叫 userService.register(username, password)
        userService.register(authRequest.getUsername(), authRequest.getPassword());
        // 成功回 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest authRequest) {
    // 呼叫 userService.login(username, password) 拿到 User
        User user = userService.login(authRequest.getUsername(), authRequest.getPassword());
    // 再呼叫 jwtUtil.generateToken(username) 產 JWT
        String token = jwtUtil.generateToken(user.getUsername());
    // 回傳 JWT 字串（或包成 JSON）
        return ResponseEntity.ok().body(token);
    }
}
