
package com.featureflag.featureflagbackend.controller;

import com.featureflag.featureflagbackend.dto.ProjectResponse;
import com.featureflag.featureflagbackend.service.ProjectService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> getAllProjects(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return projectService.getAllProjects(userDetails.getUsername())
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ProjectResponse.from(
                projectService.getProjectById(
                        id,
                        userDetails.getUsername()
                )
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ProjectResponse.from(
                projectService.createProject(
                        request.name(),
                        request.projectKey(),
                        request.description(),
                        userDetails.getUsername()
                )
        );
    }


    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ProjectResponse.from(
                projectService.updateProject(
                        id,
                        request.name(),
                        request.projectKey(),
                        request.description(),
                        userDetails.getUsername()
                )
        );
    }


    @DeleteMapping("/{id}")
    public String deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        projectService.deleteProject(
                id,
                userDetails.getUsername()
        );

        return "Project deleted successfully";
    }

    public record CreateProjectRequest(

            @NotBlank(message = "Project name is required")
            @Size(max = 150)
            String name,

            @NotBlank(message = "Project key is required")
            @Size(max = 100)
            String projectKey,

            String description

    ) {}
}
