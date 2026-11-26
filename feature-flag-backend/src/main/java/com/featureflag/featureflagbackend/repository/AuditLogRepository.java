
package com.featureflag.featureflagbackend.repository;

import com.featureflag.featureflagbackend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<AuditLog> findByFeatureFlagIdOrderByCreatedAtDesc(UUID featureFlagId);
}
