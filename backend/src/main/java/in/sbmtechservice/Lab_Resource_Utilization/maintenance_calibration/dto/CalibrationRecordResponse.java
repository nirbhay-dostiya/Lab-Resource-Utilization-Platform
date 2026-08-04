package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a Calibration Record.
 * Includes complianceStatus and daysUntilExpiry for the compliance dashboard grid.
 */
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
    private String toleranceMetrics;

    // ── Compliance Dashboard Fields ──
    /** Badge: COMPLIANT | EXPIRING_SOON | EXPIRED | NON_COMPLIANT */
    private String complianceStatus;

    /** Days until expiry — negative means already expired. */
    private Long daysUntilExpiry;
}