
package com.featureflag.featureflagbackend.controller;

import com.featureflag.featureflagbackend.dto.AuditLogResponse;
import com.featureflag.featureflagbackend.service.AuditLogService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // Get all audit logs for a project
    @GetMapping("/api/projects/{projectId}/audit-logs")
    public List<AuditLogResponse> getLogsByProject(
            @PathVariable java.util.UUID projectId) {

        return auditLogService.getLogsByProject(projectId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    // Get all audit logs for a feature flag
    @GetMapping("/api/flags/{flagId}/audit-logs")
    public List<AuditLogResponse> getLogsByFeatureFlag(
            @PathVariable java.util.UUID flagId) {

        return auditLogService.getLogsByFeatureFlag(flagId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
