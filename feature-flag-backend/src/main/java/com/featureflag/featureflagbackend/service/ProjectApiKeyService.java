
package com.featureflag.featureflagbackend.service;

import com.featureflag.featureflagbackend.entity.Project;
import com.featureflag.featureflagbackend.entity.ProjectApiKey;
import com.featureflag.featureflagbackend.repository.ProjectApiKeyRepository;
import com.featureflag.featureflagbackend.repository.ProjectRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectApiKeyService {

    private final ProjectApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public record CreatedApiKey(
            ProjectApiKey apiKey,
            String rawKey
    ) {}

    public ProjectApiKeyService(
            ProjectApiKeyRepository apiKeyRepository,
            ProjectRepository projectRepository
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.projectRepository = projectRepository;
    }

    // Get the email of the currently authenticated user
    private String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null) {

            throw new AccessDeniedException("Authentication required");
        }

        return authentication.getName();
    }

    // Verify that the current user owns the requested project
    private Project getOwnedProject(UUID projectId) {

        String email = getCurrentUserEmail();

        return projectRepository
                .findByIdAndCreatedByEmail(projectId, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Project not found"
                        )
                );
    }

    // Generate a cryptographically secure API key
    private String generateRawKey() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return "ff_live_" +
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);
    }

    // SHA-256 hashing
    private String hashKey(String rawKey) {

        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // Create an API key only for a project owned by the current user
    // Create an API key only for a project owned by the current user
    @Transactional
    public CreatedApiKey createApiKey(
            UUID projectId,
            String keyName,
            LocalDateTime expiresAt
    ) {

        Project project = getOwnedProject(projectId);

        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException(
                    "API key name is required"
            );
        }

        if (expiresAt != null &&
                !expiresAt.isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Expiration must be in the future"
            );
        }

        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);

        ProjectApiKey apiKey = new ProjectApiKey();

        apiKey.setProject(project);
        apiKey.setName(keyName.trim());
        apiKey.setKeyPrefix(rawKey.substring(0, 15));
        apiKey.setKeyHash(keyHash);
        apiKey.setExpiresAt(expiresAt);
        apiKey.setRevoked(false);

        // Save and capture the persisted entity, including its UUID
        ProjectApiKey savedApiKey = apiKeyRepository.save(apiKey);

        // Return the saved entity and raw key
        return new CreatedApiKey(savedApiKey, rawKey);
    }

    // Validate an API key for a particular project
    @Transactional(readOnly = true)
    public boolean isValidApiKey(String rawKey, UUID projectId) {

        if (rawKey == null || rawKey.isBlank()) {
            return false;
        }

        String keyHash = hashKey(rawKey);

        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(apiKey -> !apiKey.isRevoked())
                .filter(apiKey ->
                        apiKey.getExpiresAt() == null ||
                                apiKey.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(apiKey ->
                        apiKey.getProject().getId().equals(projectId))
                .isPresent();
    }

    // List API keys only for a project owned by the current user
    @Transactional(readOnly = true)
    public List<ProjectApiKey> getApiKeys(UUID projectId) {

        getOwnedProject(projectId);

        return apiKeyRepository.findByProjectId(projectId);
    }

    // Revoke an API key only if the current user owns its project
    @Transactional
    public void revokeApiKey(UUID keyId, UUID projectId) {

        getOwnedProject(projectId);

        ProjectApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "API key not found"
                        )
                );

        if (!apiKey.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "API key not found"
            );
        }

        apiKey.setRevoked(true);

        apiKeyRepository.save(apiKey);
    }
}
