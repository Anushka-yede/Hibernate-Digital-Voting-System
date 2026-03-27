package com.securevote.backend.service;

import com.securevote.backend.entity.RefreshToken;
import com.securevote.backend.entity.User;
import com.securevote.backend.exception.BusinessException;
import com.securevote.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String issueToken(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        token.setRevoked(false);
        return refreshTokenRepository.save(token).getToken();
    }

    @Transactional(readOnly = true)
    public User validateAndGetUser(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expired or revoked");
        }

        return token.getUser();
    }

    @Transactional
    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
