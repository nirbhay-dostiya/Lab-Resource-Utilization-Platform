package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.CalibrationRecord;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.CalibrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalibrationRecordRepository extends JpaRepository<CalibrationRecord, UUID> {

    // Fetch the full calibration history for a specific piece of equipment
    List<CalibrationRecord> findByEquipmentIdOrderByCalibrationDateDesc(UUID equipmentId);

    // Fetch the absolute most recent calibration record to check current compliance
    Optional<CalibrationRecord> findFirstByEquipmentIdOrderByExpiryDateDesc(UUID equipmentId);

    // Find records by their compliance status
    List<CalibrationRecord> findByStatus(CalibrationStatus status);

    // 🚨 COMPLIANCE ALERT LOGIC: Find equipment whose calibration is expiring within a date range (e.g., next 30 days)
    List<CalibrationRecord> findByExpiryDateBetweenOrderByExpiryDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    // Cron job: find records expiring on a specific date (for precise 30/14/7/1-day reminders)
    List<CalibrationRecord> findByExpiryDate(LocalDate expiryDate);

    // All latest records per equipment for compliance dashboard
    @org.springframework.data.jpa.repository.Query(
        "SELECT cr FROM CalibrationRecord cr WHERE cr.expiryDate = " +
        "(SELECT MAX(cr2.expiryDate) FROM CalibrationRecord cr2 WHERE cr2.equipment.id = cr.equipment.id) " +
        "ORDER BY cr.expiryDate ASC")
    List<CalibrationRecord> findLatestPerEquipment();
}