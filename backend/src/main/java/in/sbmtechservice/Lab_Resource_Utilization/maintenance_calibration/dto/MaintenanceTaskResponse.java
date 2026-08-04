package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenancePriority;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a Work Order / Maintenance Task.
 * Includes all fields needed to render the Kanban board card.
 */
@Data
@Builder
public class MaintenanceTaskResponse {

    private UUID id;
    private UUID equipmentId;
    private String equipmentName;
    private UUID technicianId;
    private String technicianName;
    private UUID requesterId;
    private String requesterName;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private MaintenancePriority priority;
    private LocalDateTime scheduledDate;
    private LocalDateTime completedDate;
    private String description;
    private String resolutionNotes;
    private BigDecimal downtimeHours;
    private BigDecimal cost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}