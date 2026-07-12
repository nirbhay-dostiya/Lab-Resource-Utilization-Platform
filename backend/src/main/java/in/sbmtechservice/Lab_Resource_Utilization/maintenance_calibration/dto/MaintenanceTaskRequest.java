package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MaintenanceTaskRequest {
    private UUID equipmentId;
    private UUID technicianId; // Optional
    private MaintenanceType maintenanceType;
    private LocalDateTime scheduledDate;
    private String description; // Required in your entity!
    private BigDecimal estimatedCost;
}