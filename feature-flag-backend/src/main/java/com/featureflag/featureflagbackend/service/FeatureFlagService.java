
package com.featureflag.featureflagbackend.service;

import com.featureflag.featureflagbackend.entity.FeatureFlag;
import com.featureflag.featureflagbackend.entity.Project;
import com.featureflag.featureflagbackend.repository.FeatureFlagRepository;
import com.featureflag.featureflagbackend.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;

    public FeatureFlagService(
            FeatureFlagRepository featureFlagRepository,
            ProjectRepository projectRepository,
            AuditLogService auditLogService
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    public List<FeatureFlag> getFlagsByProject(UUID projectId) {
        return featureFlagRepository.findByProjectId(projectId);
    }

    public FeatureFlag getFlagById(UUID id) {
        return featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature flag not found"));
    }

    @Transactional
    public FeatureFlag createFlag(
            UUID projectId,
            String name,
            String flagKey,
            String description,
            boolean enabled
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (featureFlagRepository.existsByProjectIdAndFlagKey(projectId, flagKey)) {
            throw new RuntimeException("Feature flag key already exists");
        }

        FeatureFlag flag = new FeatureFlag();
        flag.setProject(project);
        flag.setName(name);
        flag.setFlagKey(flagKey);
        flag.setDescription(description);
        flag.setIsEnabled(enabled   );

        FeatureFlag savedFlag = featureFlagRepository.save(flag);

        auditLogService.logAction(
                null,
                project,
                savedFlag,
                "FLAG_CREATED",
                null,
                auditLogService.createFlagSnapshot(savedFlag)
        );

        return savedFlag;
    }

    @Transactional
    public FeatureFlag updateFlagStatus(UUID id, boolean enabled) {
        FeatureFlag flag = getFlagById(id);

        Map<String, Object> oldValue =
                auditLogService.createFlagSnapshot(flag);

        flag.setIsEnabled(enabled);

        FeatureFlag updatedFlag = featureFlagRepository.save(flag);

        auditLogService.logAction(
                null,
                updatedFlag.getProject(),
                updatedFlag,
                enabled ? "FLAG_ENABLED" : "FLAG_DISABLED",
                oldValue,
                auditLogService.createFlagSnapshot(updatedFlag)
        );

        return updatedFlag;
    }

    @Transactional
    public void deleteFlag(UUID id) {
        FeatureFlag flag = getFlagById(id);

        Map<String, Object> oldValue =
                auditLogService.createFlagSnapshot(flag);

        auditLogService.logAction(
                null,
                flag.getProject(),
                null,
                "FLAG_DELETED",
                oldValue,
                null
        );

        featureFlagRepository.delete(flag);
    }
}
