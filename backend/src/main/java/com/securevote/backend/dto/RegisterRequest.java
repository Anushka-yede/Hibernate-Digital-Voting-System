package com.securevote.backend.dto;

import java.time.LocalDate;

import com.securevote.backend.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    private Role role = Role.USER;
}
