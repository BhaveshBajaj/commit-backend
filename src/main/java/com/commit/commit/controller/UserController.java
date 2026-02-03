package com.commit.commit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commit.commit.dto.UserResponse;
import com.commit.commit.security.AuthenticatedUser;
import com.commit.commit.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser(AuthenticatedUser.getUserId());
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }
}
