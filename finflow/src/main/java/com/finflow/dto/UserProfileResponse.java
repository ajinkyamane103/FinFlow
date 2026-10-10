package com.finflow.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(Long id, String fullName, String email, LocalDateTime createdAt) {
}
