package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MaintenanceTaskResponse {
    private UUID id;
    private UUID equipmentId;
    private String equipmentName;
    private String technicianName;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private LocalDateTime scheduledDate;
    private LocalDateTime completedDate;
    private String description;
    private String resolutionNotes;
    private BigDecimal cost;
}