
package com.featureflag.featureflagbackend.controller;

import com.featureflag.featureflagbackend.entity.ProjectApiKey;
import com.featureflag.featureflagbackend.service.ProjectApiKeyService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/api-keys")
public class ProjectApiKeyController {

    private final ProjectApiKeyService apiKeyService;

    public ProjectApiKeyController(ProjectApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    // Create API key
    // Create API key
    @PostMapping
    public ResponseEntity<Map<String, Object>> createApiKey(
            @PathVariable UUID projectId,
            @RequestBody CreateApiKeyRequest request
    ) {

        ProjectApiKeyService.CreatedApiKey created =
                apiKeyService.createApiKey(
                        projectId,
                        request.name(),
                        request.expiresAt()
                );

        ProjectApiKey savedApiKey = created.apiKey();
        String rawKey = created.rawKey();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", savedApiKey.getId(),
                        "name", savedApiKey.getName(),
                        "keyPrefix", savedApiKey.getKeyPrefix(),
                        "message", "API key created successfully",
                        "apiKey", rawKey,
                        "warning", "Store this key securely. It will not be displayed again."
                )
        );
    }

    // List API keys
    @GetMapping
    public List<Map<String, Object>> getApiKeys(
            @PathVariable UUID projectId
    ) {

        return apiKeyService.getApiKeys(projectId)
                .stream()
                .map(this::toSafeResponse)
                .toList();
    }

    // Revoke API key
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Map<String, String>> revokeApiKey(
            @PathVariable UUID projectId,
            @PathVariable UUID keyId
    ) {

        apiKeyService.revokeApiKey(keyId, projectId);

        return ResponseEntity.ok(
                Map.of("message", "API key revoked successfully")
        );
    }

    private Map<String, Object> toSafeResponse(ProjectApiKey apiKey) {
        return Map.of(
                "id", apiKey.getId(),
                "name", apiKey.getName(),
                "keyPrefix", apiKey.getKeyPrefix(),
                "createdAt", apiKey.getCreatedAt(),
                "expiresAt", apiKey.getExpiresAt() == null
                        ? ""
                        : apiKey.getExpiresAt(),
                "revoked", apiKey.isRevoked()
        );
    }

    public record CreateApiKeyRequest(
            String name,
            LocalDateTime expiresAt
    ) {}
}
