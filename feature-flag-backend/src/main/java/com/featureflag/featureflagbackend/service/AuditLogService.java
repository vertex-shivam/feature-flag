
package com.featureflag.featureflagbackend.service;

import com.featureflag.featureflagbackend.entity.AuditLog;
import com.featureflag.featureflagbackend.entity.FeatureFlag;
import com.featureflag.featureflagbackend.entity.Project;
import com.featureflag.featureflagbackend.entity.User;
import com.featureflag.featureflagbackend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(
            User user,
            Project project,
            FeatureFlag featureFlag,
            String action,
            Map<String, Object> oldValue,
            Map<String, Object> newValue
    ) {
        AuditLog auditLog = new AuditLog();

        auditLog.setUser(user);
        auditLog.setProject(project);
        auditLog.setFeatureFlag(featureFlag);
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);

        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getLogsByProject(UUID projectId) {
        return auditLogRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public List<AuditLog> getLogsByFeatureFlag(UUID flagId) {
        return auditLogRepository
                .findByFeatureFlagIdOrderByCreatedAtDesc(flagId);
    }

    public Map<String, Object> createFlagSnapshot(FeatureFlag flag) {
        Map<String, Object> snapshot = new HashMap<>();

        snapshot.put("id", flag.getId());
        snapshot.put("name", flag.getName());
        snapshot.put("flagKey", flag.getFlagKey());
        snapshot.put("description", flag.getDescription());
        snapshot.put("isEnabled", flag.getIsEnabled());

        return snapshot;
    }
}
