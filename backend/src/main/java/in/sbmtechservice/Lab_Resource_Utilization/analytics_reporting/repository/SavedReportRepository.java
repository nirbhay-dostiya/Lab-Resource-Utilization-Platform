package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.repository;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.entity.SavedReport;
import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedReportRepository extends JpaRepository<SavedReport, UUID> {

    // Fetch all report templates saved by a specific user
    List<SavedReport> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Fetch a user's saved reports filtered by the type of report (e.g., only "FINANCIAL" reports)
    List<SavedReport> findByUserIdAndReportTypeOrderByCreatedAtDesc(UUID userId, ReportType reportType);
}