package com.securevote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    @NotBlank
    @Size(min = 3, max = 150)
    private String usernameOrEmail;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;
}
