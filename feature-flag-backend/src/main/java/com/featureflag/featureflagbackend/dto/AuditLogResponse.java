
package com.featureflag.featureflagbackend.dto;

import com.featureflag.featureflagbackend.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        UUID projectId,
        UUID featureFlagId,
        String action,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        LocalDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {

        return new AuditLogResponse(
                auditLog.getId(),

                auditLog.getUser() == null
                        ? null
                        : auditLog.getUser().getId(),

                auditLog.getProject() == null
                        ? null
                        : auditLog.getProject().getId(),

                auditLog.getFeatureFlag() == null
                        ? null
                        : auditLog.getFeatureFlag().getId(),

                auditLog.getAction(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getCreatedAt()
        );
    }
}
