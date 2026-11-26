
package com.featureflag.featureflagbackend.repository;

import com.featureflag.featureflagbackend.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    List<FeatureFlag> findByProjectId(UUID projectId);

    Optional<FeatureFlag> findByProjectIdAndFlagKey(UUID projectId, String flagKey);

    boolean existsByProjectIdAndFlagKey(UUID projectId, String flagKey);
}
