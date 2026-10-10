package com.finflow.service;

import com.finflow.dto.UserProfileResponse;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getRequiredUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User was not found"));
    }

    public UserProfileResponse getProfile(String email) {
        User user = getRequiredUser(email);
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }
}
