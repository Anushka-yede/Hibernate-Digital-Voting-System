package com.securevote.backend.service;

import com.securevote.backend.dto.AuthRequest;
import com.securevote.backend.dto.AuthResponse;
import com.securevote.backend.dto.RegisterRequest;
import com.securevote.backend.dto.RefreshTokenRequest;
import com.securevote.backend.entity.Role;
import com.securevote.backend.entity.User;
import com.securevote.backend.exception.BusinessException;
import com.securevote.backend.repository.UserRepository;
import com.securevote.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
        private final RefreshTokenService refreshTokenService;
        private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                                           JwtService jwtService,
                                           RefreshTokenService refreshTokenService,
                                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
                this.refreshTokenService = refreshTokenService;
                this.auditLogService = auditLogService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setRole(Role.USER);

        validateAdult(user.getDateOfBirth());

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.withUsername(saved.getUsername())
                        .password(saved.getPassword())
                        .roles(saved.getRole().name())
                        .build(),
                Map.of("role", saved.getRole().name(), "uid", saved.getId())
        );
        String refreshToken = refreshTokenService.issueToken(saved);
        auditLogService.log(saved.getUsername(), "REGISTER", "User registered successfully");

        return new AuthResponse(token, refreshToken, "Bearer", saved.getId(), saved.getUsername(), saved.getRole());
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        validateAdult(user.getDateOfBirth());

        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build(),
                Map.of("role", user.getRole().name(), "uid", user.getId())
        );
        String refreshToken = refreshTokenService.issueToken(user);
        auditLogService.log(user.getUsername(), "LOGIN", "User authenticated successfully");

        return new AuthResponse(token, refreshToken, "Bearer", user.getId(), user.getUsername(), user.getRole());
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        User user = refreshTokenService.validateAndGetUser(request.getRefreshToken());
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build(),
                Map.of("role", user.getRole().name(), "uid", user.getId())
        );
        String nextRefreshToken = refreshTokenService.issueToken(user);
        auditLogService.log(user.getUsername(), "TOKEN_REFRESH", "Access token rotated");
        return new AuthResponse(token, nextRefreshToken, "Bearer", user.getId(), user.getUsername(), user.getRole());
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

        private void validateAdult(LocalDate dateOfBirth) {
                if (dateOfBirth == null) {
                        throw new BusinessException("Date of birth is required");
                }

                int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
                if (age < 18) {
                        throw new BusinessException("You must be at least 18 years old to access the platform");
                }
        }
}
