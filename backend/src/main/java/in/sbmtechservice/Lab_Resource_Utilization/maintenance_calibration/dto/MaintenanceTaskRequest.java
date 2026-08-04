package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenancePriority;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request payload for creating a new Work Order / Maintenance Task.
 * All fields validated via Jakarta Bean Validation.
 */
@Data
public class MaintenanceTaskRequest {

    @NotNull(message = "Equipment ID is required")
    private UUID equipmentId;

    private UUID technicianId;

    private UUID requesterId;

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    @NotNull(message = "Priority is required")
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    @NotNull(message = "Scheduled date is required")
    private LocalDateTime scheduledDate;

    @NotBlank(message = "Description is required")
    private String description;

    private BigDecimal estimatedCost;

    private BigDecimal downtimeHours;
}