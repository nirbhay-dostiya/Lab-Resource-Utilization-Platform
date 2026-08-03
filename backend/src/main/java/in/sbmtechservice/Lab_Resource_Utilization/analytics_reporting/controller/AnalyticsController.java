package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.controller;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.AnalyticsResponse;
import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.BookingResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.EquipmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // --- Drill-Down Endpoints ---

    @GetMapping("/global/equipment")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Page<EquipmentResponse>> getGlobalEquipmentDetails(
            @RequestParam(required = false) EquipmentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getEquipmentDetails("global", null, status, pageable));
    }

    @GetMapping("/global/bookings")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Page<BookingResponse>> getGlobalBookingDetails(
            @RequestParam(required = false) BookingStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getBookingDetails("global", null, status, pageable));
    }

    @GetMapping("/institution/{institutionId}/equipment")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<Page<EquipmentResponse>> getInstitutionEquipmentDetails(
            @PathVariable UUID institutionId,
            @RequestParam(required = false) EquipmentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getEquipmentDetails("institution", institutionId, status, pageable));
    }

    @GetMapping("/institution/{institutionId}/bookings")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<Page<BookingResponse>> getInstitutionBookingDetails(
            @PathVariable UUID institutionId,
            @RequestParam(required = false) BookingStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getBookingDetails("institution", institutionId, status, pageable));
    }

    @GetMapping("/department/{departmentId}/equipment")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<Page<EquipmentResponse>> getDepartmentEquipmentDetails(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) EquipmentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getEquipmentDetails("department", departmentId, status, pageable));
    }

    @GetMapping("/department/{departmentId}/bookings")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<Page<BookingResponse>> getDepartmentBookingDetails(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) BookingStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(analyticsService.getBookingDetails("department", departmentId, status, pageable));
    }
}
