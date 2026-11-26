
package com.featureflag.featureflagbackend.service;

import com.featureflag.featureflagbackend.entity.Project;
import com.featureflag.featureflagbackend.entity.User;
import com.featureflag.featureflagbackend.repository.ProjectRepository;
import com.featureflag.featureflagbackend.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public List<Project> getAllProjects(String userEmail) {
        return projectRepository.findByCreatedByEmail(userEmail);
    }

    public Project getProjectById(UUID id, String userEmail) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        if (!project.getCreatedBy().getEmail().equals(userEmail)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this project");
        }

        return project;
    }

    public Project createProject(
            String name,
            String projectKey,
            String description,
            String userEmail) {

        if (projectRepository.existsByProjectKey(projectKey)) {
            throw new RuntimeException("Project key already exists");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Project project = new Project();
        project.setName(name);
        project.setProjectKey(projectKey);
        project.setDescription(description);
        project.setCreatedBy(user);

        return projectRepository.save(project);
    }

    public Project updateProject(
            UUID id,
            String name,
            String projectKey,
            String description,
            String userEmail) {

        // Verify project ownership
        Project project = getProjectById(id, userEmail);

        // Check whether another project uses this key
        if (!project.getProjectKey().equals(projectKey)
                && projectRepository.existsByProjectKey(projectKey)) {
            throw new RuntimeException("Project key already exists");
        }

        project.setName(name);
        project.setProjectKey(projectKey);
        project.setDescription(description);

        return projectRepository.save(project);
    }

    public void deleteProject(UUID id, String userEmail) {

        Project project = getProjectById(id, userEmail);
        projectRepository.delete(project);
    }
}
