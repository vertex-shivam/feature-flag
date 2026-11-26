
package com.featureflag.featureflagbackend.controller;

import com.featureflag.featureflagbackend.dto.FeatureFlagResponse;
import com.featureflag.featureflagbackend.entity.FeatureFlag;
import com.featureflag.featureflagbackend.service.FeatureFlagService;
import com.featureflag.featureflagbackend.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/flags")
@CrossOrigin(origins = "*")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final ProjectService projectService;

    public FeatureFlagController(
            FeatureFlagService featureFlagService,
            ProjectService projectService) {
        this.featureFlagService = featureFlagService;
        this.projectService = projectService;
    }

    // Get all flags belonging to an owned project
    @GetMapping
    public List<FeatureFlagResponse> getFlagsByProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        projectService
                .getProjectById(projectId, userDetails.getUsername());

        return featureFlagService.getFlagsByProject(projectId)
                .stream()
                .map(FeatureFlagResponse::from)
                .toList();
    }

    // Get one specific feature flag
    @GetMapping("/{flagId}")
    public FeatureFlagResponse getFlagById(
            @PathVariable UUID projectId,
            @PathVariable UUID flagId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        FeatureFlag flag = getAndValidateFlag(
                projectId, flagId, userDetails.getUsername()
        );

        return FeatureFlagResponse.from(flag);
    }

    // Create a feature flag
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeatureFlagResponse createFlag(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateFlagRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        projectService
                .getProjectById(projectId, userDetails.getUsername());

        FeatureFlag flag = featureFlagService.createFlag(
                projectId,
                request.name(),
                request.flagKey(),
                request.description(),
                request.enabled()
        );

        return FeatureFlagResponse.from(flag);
    }

    // Enable or disable a feature flag
    @PatchMapping("/{flagId}/status")
    public FeatureFlagResponse updateFlagStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID flagId,
            @RequestBody UpdateFlagStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        getAndValidateFlag(
                projectId, flagId, userDetails.getUsername()
        );

        FeatureFlag updatedFlag = featureFlagService.updateFlagStatus(
                flagId,
                request.enabled()
        );

        return FeatureFlagResponse.from(updatedFlag);
    }

    // Delete a feature flag
    @DeleteMapping("/{flagId}")
    public String deleteFlag(
            @PathVariable UUID projectId,
            @PathVariable UUID flagId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        getAndValidateFlag(
                projectId, flagId, userDetails.getUsername()
        );

        featureFlagService.deleteFlag(flagId);

        return "Feature flag deleted successfully";
    }

    // Verify project ownership and flag-project relationship
    private FeatureFlag getAndValidateFlag(
            UUID projectId,
            UUID flagId,
            String userEmail
    ) {
        projectService
                .getProjectById(projectId, userEmail);

        FeatureFlag flag = featureFlagService.getFlagById(flagId);

        if (!flag.getProject().getId().equals(projectId)) {
            throw new AccessDeniedException(
                    "Feature flag does not belong to this project"
            );
        }

        return flag;
    }

    public record CreateFlagRequest(
            @NotBlank(message = "Feature flag name is required")
            @Size(max = 150)
            String name,

            @NotBlank(message = "Feature flag key is required")
            @Size(max = 150)
            String flagKey,

            String description,

            boolean enabled
    ) {}

    public record UpdateFlagStatusRequest(
            boolean enabled
    ) {}
}
