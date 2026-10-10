
package com.finflow.controller;

import com.finflow.dto.RegisterRequest;
import com.finflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "FinFlow account registered successfully"
                ));
    }


    @PostMapping("/login")
    public ResponseEntity<com.finflow.dto.AuthResponse> login(
            @Valid @RequestBody com.finflow.dto.LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

}
