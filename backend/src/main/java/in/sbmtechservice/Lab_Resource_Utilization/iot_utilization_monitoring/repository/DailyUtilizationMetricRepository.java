package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository;

import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.DailyUtilizationMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyUtilizationMetricRepository extends JpaRepository<DailyUtilizationMetric, UUID> {

    // Fetch the specific metric record for a given day (Useful for upsert logic)
    Optional<DailyUtilizationMetric> findByEquipmentIdAndRecordDate(UUID equipmentId, LocalDate recordDate);

    // Fetch metrics over a date range (e.g., for Monthly/Weekly Reports)
    List<DailyUtilizationMetric> findByEquipmentIdAndRecordDateBetweenOrderByRecordDateAsc(
            UUID equipmentId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Fetch the worst-performing equipment by utilization rate for a specific date
    List<DailyUtilizationMetric> findTop10ByRecordDateOrderByUtilizationRateAsc(LocalDate recordDate);

    // Fetch the best-performing equipment by utilization rate for a specific date
    List<DailyUtilizationMetric> findTop10ByRecordDateOrderByUtilizationRateDesc(LocalDate recordDate);
}