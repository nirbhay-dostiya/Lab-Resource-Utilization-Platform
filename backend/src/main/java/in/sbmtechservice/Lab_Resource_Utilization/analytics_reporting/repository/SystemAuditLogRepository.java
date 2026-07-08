package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.repository;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.entity.SystemAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {

    // Fetch the global system audit trail (Paginated for admin dashboards)
    Page<SystemAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Fetch the audit trail for a specific record (e.g., "Show me all changes to Booking ID 123")
    Page<SystemAuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(
            String entityName,
            UUID entityId,
            Pageable pageable
    );

    // Fetch all actions performed by a specific user (Useful for security investigations)
    Page<SystemAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Fetch logs within a specific timeframe
    Page<SystemAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}