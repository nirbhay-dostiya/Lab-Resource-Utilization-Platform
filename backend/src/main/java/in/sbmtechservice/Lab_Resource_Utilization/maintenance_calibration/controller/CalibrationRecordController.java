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
}