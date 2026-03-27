package com.securevote.backend.dto;

import com.securevote.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private Role role;
}
