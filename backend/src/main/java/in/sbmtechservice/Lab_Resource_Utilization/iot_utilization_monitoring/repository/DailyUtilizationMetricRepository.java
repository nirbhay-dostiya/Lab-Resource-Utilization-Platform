package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository;

import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.DailyUtilizationMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.DailyAggregatedUtilization;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.EquipmentUtilizationStat;

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

    @Query("SELECT new in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.DailyAggregatedUtilization(" +
           "m.recordDate, AVG(m.utilizationRate), SUM(CAST(m.idleTimeMinutes AS long)), SUM(CAST(m.totalBookedMinutes AS long)), SUM(CAST(m.totalUsedMinutes AS long))) " +
           "FROM DailyUtilizationMetric m " +
           "WHERE m.recordDate BETWEEN :startDate AND :endDate " +
           "GROUP BY m.recordDate " +
           "ORDER BY m.recordDate ASC")
    List<DailyAggregatedUtilization> getAggregatedMetrics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT new in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.EquipmentUtilizationStat(" +
           "m.equipment.id, m.equipment.name, AVG(m.utilizationRate), SUM(CAST(m.idleTimeMinutes AS long))) " +
           "FROM DailyUtilizationMetric m " +
           "WHERE m.recordDate BETWEEN :startDate AND :endDate " +
           "GROUP BY m.equipment.id, m.equipment.name " +
           "ORDER BY AVG(m.utilizationRate) DESC")
    List<EquipmentUtilizationStat> getTopUtilizedEquipment(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT new in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.EquipmentUtilizationStat(" +
           "m.equipment.id, m.equipment.name, AVG(m.utilizationRate), SUM(CAST(m.idleTimeMinutes AS long))) " +
           "FROM DailyUtilizationMetric m " +
           "WHERE m.recordDate BETWEEN :startDate AND :endDate " +
           "GROUP BY m.equipment.id, m.equipment.name " +
           "ORDER BY AVG(m.utilizationRate) ASC")
    List<EquipmentUtilizationStat> getLeastUtilizedEquipment(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}