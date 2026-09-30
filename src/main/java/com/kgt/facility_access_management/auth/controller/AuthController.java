package com.kgt.facility_access_management.auth.controller;

import com.kgt.facility_access_management.auth.dto.LoginRequestForm;
import com.kgt.facility_access_management.auth.service.AuthService;
import com.kgt.facility_access_management.user.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody LoginRequestForm request,
            HttpSession session
            ) {
        Optional<User> loginUser = authService.login(request.getLoginId(), request.getPassword());

        if (loginUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }

        User user = loginUser.get();

        session.setAttribute("LOGIN_USER", user);

        return ResponseEntity.ok("로그인 성공");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃 성공");
    }




}
