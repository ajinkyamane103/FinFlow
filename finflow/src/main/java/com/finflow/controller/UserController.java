package com.finflow.controller;

import com.finflow.dto.UserProfileResponse;
import com.finflow.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }
}
