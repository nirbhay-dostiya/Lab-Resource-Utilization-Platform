package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.controller;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.CalibrationRecordRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.CalibrationRecordResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service.CalibrationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calibration")
@RequiredArgsConstructor
public class CalibrationRecordController {

    private final CalibrationRecordService calibrationService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<CalibrationRecordResponse> logCalibration(@RequestBody CalibrationRecordRequest request) {
        return ResponseEntity.ok(calibrationService.logCalibration(request));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<java.util.List<CalibrationRecordResponse>> getRecordsByEquipment(@PathVariable java.util.UUID equipmentId) {
        return ResponseEntity.ok(calibrationService.getRecordsByEquipment(equipmentId));
    }

    @GetMapping
    public ResponseEntity<java.util.List<CalibrationRecordResponse>> getAllRecords() {
        return ResponseEntity.ok(calibrationService.getAllRecords());
    }

    /**
     * Compliance Dashboard endpoint.
     * Returns one record per equipment (the latest), with computed complianceStatus badge.
     * Badges: COMPLIANT | EXPIRING_SOON | EXPIRED | NON_COMPLIANT
     */
    @GetMapping("/compliance-dashboard")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<java.util.List<CalibrationRecordResponse>> getComplianceDashboard() {
        return ResponseEntity.ok(calibrationService.getComplianceDashboard());
    }
}