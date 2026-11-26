
package com.featureflag.featureflagbackend.repository;

import com.featureflag.featureflagbackend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByProjectKey(String projectKey);

    List<Project> findByCreatedById(UUID userId);

    List<Project> findByCreatedByEmail(String email);

    boolean existsByProjectKey(String projectKey);

    Optional<Project> findByIdAndCreatedByEmail(UUID id, String email);
}
