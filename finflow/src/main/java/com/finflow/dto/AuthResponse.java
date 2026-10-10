
package com.finflow.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
