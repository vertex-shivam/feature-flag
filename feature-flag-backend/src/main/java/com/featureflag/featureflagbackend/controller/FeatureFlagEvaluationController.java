
package com.featureflag.featureflagbackend.controller;

import com.featureflag.featureflagbackend.entity.FeatureFlag;
import com.featureflag.featureflagbackend.repository.FeatureFlagRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluate")
@CrossOrigin(origins = "*")
public class FeatureFlagEvaluationController {

    private final FeatureFlagRepository featureFlagRepository;

    public FeatureFlagEvaluationController(
            FeatureFlagRepository featureFlagRepository
    ) {
        this.featureFlagRepository = featureFlagRepository;
    }

    // Evaluate whether a feature flag is enabled
    @GetMapping("/{projectId}/{flagKey}")
    public Map<String, Object> evaluateFlag(
            @PathVariable UUID projectId,
            @PathVariable String flagKey
    ) {
        FeatureFlag flag = featureFlagRepository
                .findByProjectIdAndFlagKey(projectId, flagKey)
                .orElse(null);

        Map<String, Object> response = new HashMap<>();

        if (flag == null) {
            response.put("projectId", projectId);
            response.put("flagKey", flagKey);
            response.put("enabled", false);
            response.put("message", "Feature flag not found");
            return response;
        }

        response.put("projectId", projectId);
        response.put("flagKey", flagKey);
        response.put("enabled", flag.getIsEnabled());
        response.put("message", "Feature flag evaluated successfully");

        return response;
    }
}
