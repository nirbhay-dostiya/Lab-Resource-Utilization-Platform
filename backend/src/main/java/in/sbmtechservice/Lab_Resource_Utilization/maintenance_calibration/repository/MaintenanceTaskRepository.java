package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.MaintenanceTask;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, UUID> {

    // Fetch the maintenance history for a specific piece of equipment
    List<MaintenanceTask> findByEquipmentIdOrderByScheduledDateDesc(UUID equipmentId);

    // Fetch all tasks assigned to a specific technician (Useful for their personal dashboard)
    List<MaintenanceTask> findByTechnicianIdOrderByScheduledDateAsc(UUID technicianId);

    // Find tasks by their current status (e.g., find all 'SCHEDULED' tasks)
    List<MaintenanceTask> findByStatus(MaintenanceStatus status);

    // Fetch tasks scheduled within a specific time window (Crucial for calendar views)
    List<MaintenanceTask> findByScheduledDateBetweenOrderByScheduledDateAsc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}