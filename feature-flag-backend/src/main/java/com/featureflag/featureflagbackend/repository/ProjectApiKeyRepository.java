
package com.featureflag.featureflagbackend.repository;

import com.featureflag.featureflagbackend.entity.ProjectApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectApiKeyRepository
        extends JpaRepository<ProjectApiKey, UUID> {

    Optional<ProjectApiKey> findByKeyHash(String keyHash);

    List<ProjectApiKey> findByProjectId(UUID projectId);

    boolean existsByKeyHash(String keyHash);
}
