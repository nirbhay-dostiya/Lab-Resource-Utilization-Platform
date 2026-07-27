package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository;

import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.IotTelemetryLog;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.enums.SensorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface IotTelemetryLogRepository extends JpaRepository<IotTelemetryLog, UUID> {

    // Fetch paginated logs for a specific piece of equipment (Mandatory Pageable for safety)
    Page<IotTelemetryLog> findByEquipmentIdOrderByRecordedAtDesc(UUID equipmentId, Pageable pageable);

    // Fetch logs within a specific time window (e.g., for generating a chart)
    Page<IotTelemetryLog> findByEquipmentIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            UUID equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // Fetch the absolute latest telemetry log to get the "current status" of the machine
    IotTelemetryLog findFirstByEquipmentIdOrderByRecordedAtDesc(UUID equipmentId);

    // Find logs indicating errors or faults
    Page<IotTelemetryLog> findByEquipmentIdAndSensorStatusOrderByRecordedAtDesc(
            UUID equipmentId,
            SensorStatus status,
            Pageable pageable
    );
}