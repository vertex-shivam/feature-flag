
package com.featureflag.featureflagbackend.service;

import com.featureflag.featureflagbackend.entity.RevokedToken;
import com.featureflag.featureflagbackend.repository.RevokedTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    public TokenRevocationService(
            RevokedTokenRepository revokedTokenRepository,
            JwtService jwtService) {

        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
    }

    // Generate SHA-256 hash of JWT
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash JWT", exception
            );
        }
    }

    // Revoke the JWT
    @Transactional
    public void revokeToken(String token) {

        String tokenHash = hashToken(token);

        // Avoid duplicate revocation records
        if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        Date expiration = jwtService.extractExpiration(token);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                expiration.toInstant(),
                ZoneOffset.UTC
        );

        if (!expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException(
                    "Cannot revoke an expired token"
            );
        }

        RevokedToken revokedToken = new RevokedToken();

        revokedToken.setTokenHash(tokenHash);
        revokedToken.setExpiresAt(expiresAt);

        revokedTokenRepository.save(revokedToken);
    }

    // Check whether JWT has been revoked
    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String token) {

        String tokenHash = hashToken(token);

        return revokedTokenRepository.existsByTokenHash(tokenHash);
    }

    // Remove expired revocation records
    @Transactional
    public void deleteExpiredTokens() {

        revokedTokenRepository.deleteByExpiresAtBefore(
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }
}
