package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.CalibrationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CalibrationRecordRequest {

    @NotNull(message = "Equipment ID is required")
    private UUID equipmentId;

    private UUID calibratedById;

    private String vendorName;

    @NotNull(message = "Calibration date is required")
    private LocalDate calibrationDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    private String certificateUrl;

    @NotNull(message = "Status is required")
    private CalibrationStatus status;

    /**
     * Optional JSON string with tolerance metrics.
     * Example: {"measurementRange":"0-100mV","tolerance":"±0.5%","standard":"ISO 9001"}
     */
    private String toleranceMetrics;
}