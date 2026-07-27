package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.CalibrationStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CalibrationRecordRequest {
    private UUID equipmentId;
    private UUID calibratedById; // Pass this if done internally
    private String vendorName;   // Pass this if done externally
    private LocalDate calibrationDate;
    private LocalDate expiryDate;
    private String certificateUrl;
    private CalibrationStatus status;
}