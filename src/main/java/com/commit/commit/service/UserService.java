package com.commit.commit.service;

import org.springframework.stereotype.Service;

import com.commit.commit.dto.UserResponse;
import com.commit.commit.entity.User;
import com.commit.commit.exception.NotFoundException;
import com.commit.commit.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return toResponse(user);
    }

    public UserResponse getCurrentUser(Long userId) {
        return getUserById(userId);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail()
        );
    }
}
