package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.repository;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.entity.ReportExecution;
import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, UUID> {

    // Fetch the execution history of a specific saved report template
    Page<ReportExecution> findBySavedReportIdOrderByExecutionTimeDesc(UUID savedReportId, Pageable pageable);

    // Fetch the recent report generations triggered by a specific user
    Page<ReportExecution> findByExecutedByOrderByExecutionTimeDesc(UUID userId, Pageable pageable);

    // Find report generations that failed or are currently processing (Useful for admin health checks)
    List<ReportExecution> findByStatus(ExecutionStatus status);
}