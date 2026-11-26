
package com.featureflag.featureflagbackend.dto;

import com.featureflag.featureflagbackend.entity.FeatureFlag;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        UUID projectId,
        String name,
        String flagKey,
        String description,
        Boolean isEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FeatureFlagResponse from(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(),
                flag.getProject().getId(),
                flag.getName(),
                flag.getFlagKey(),
                flag.getDescription(),
                flag.getIsEnabled(),
                flag.getCreatedAt(),
                flag.getUpdatedAt()
        );
    }
}
