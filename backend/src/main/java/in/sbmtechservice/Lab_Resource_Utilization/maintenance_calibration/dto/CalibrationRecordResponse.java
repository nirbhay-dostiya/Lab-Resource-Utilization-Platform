package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.CalibrationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CalibrationRecordResponse {
    private UUID id;
    private UUID equipmentId;
    private String equipmentName;
    private String performedBy;
    private String vendorName;
    private LocalDate calibrationDate;
    private LocalDate nextDueDate;
    private String certificateUrl;
    private String result;
}