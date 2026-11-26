
package com.featureflag.featureflagbackend.controller;

import com.featureflag.featureflagbackend.dto.AuthResponse;
import com.featureflag.featureflagbackend.dto.LoginRequest;
import com.featureflag.featureflagbackend.dto.RegisterRequest;
import com.featureflag.featureflagbackend.service.AuthService;
import com.featureflag.featureflagbackend.service.TokenRevocationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(
            AuthService authService,
            TokenRevocationService tokenRevocationService) {

        this.authService = authService;
        this.tokenRevocationService = tokenRevocationService;
    }

    // User registration
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    // User login
    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    // User logout - revoke JWT token
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bearer token is required."));
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bearer token is required."));
        }

        tokenRevocationService.revokeToken(token);

        return ResponseEntity.ok(
                Map.of("message", "Logout successful. JWT token has been revoked.")
        );
    }
}
