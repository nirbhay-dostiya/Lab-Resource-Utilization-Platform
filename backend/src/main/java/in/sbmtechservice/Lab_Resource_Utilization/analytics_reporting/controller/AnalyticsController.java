package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.controller;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.AnalyticsResponse;
import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/global")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<AnalyticsResponse> getGlobalAnalytics() {
        return ResponseEntity.ok(analyticsService.getGlobalAnalytics());
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<AnalyticsResponse> getInstitutionAnalytics(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(analyticsService.getInstitutionAnalytics(institutionId));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<AnalyticsResponse> getDepartmentAnalytics(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(analyticsService.getDepartmentAnalytics(departmentId));
    }
}
