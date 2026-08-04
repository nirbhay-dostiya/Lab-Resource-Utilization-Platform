package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.controller;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service.MaintenanceTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Work Order / Maintenance Task REST Controller.
 * All state transitions are routed through the state machine in MaintenanceTaskService.
 */
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceTaskController {

    private final MaintenanceTaskService maintenanceTaskService;

    /** Create a new Work Order. Equipment is locked to UNDER_MAINTENANCE immediately. */
    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER') or hasAuthority('LAB_TECHNICIAN')")
    public ResponseEntity<MaintenanceTaskResponse> createWorkOrder(
            @Valid @RequestBody MaintenanceTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceTaskService.scheduleTask(request));
    }

    /**
     * Transition a Work Order to a new state.
     * The state machine in MaintenanceTaskService enforces valid transitions and role gates.
     */
    @PatchMapping("/{id}/transition")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MaintenanceTaskResponse> transitionStatus(
            @PathVariable UUID id,
            @RequestParam MaintenanceStatus targetStatus,
            @RequestParam(required = false) String resolutionNotes,
            @RequestParam(required = false) BigDecimal finalCost,
            @RequestParam(required = false) BigDecimal downtimeHours,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extract role strings from Spring Security authorities
        String roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.joining(","));

        // Extract user ID from the custom UserDetails (assumes CustomUserDetails has getId())
        UUID userId = null;
        if (userDetails instanceof in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails cud) {
            userId = cud.getId();
        }

        MaintenanceTaskResponse response = maintenanceTaskService.transitionStatus(
                id, targetStatus, roles, userId, resolutionNotes, finalCost, downtimeHours);
        return ResponseEntity.ok(response);
    }

    /** Legacy complete endpoint — kept for backward compatibility. */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_TECHNICIAN') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<MaintenanceTaskResponse> completeTask(
            @PathVariable UUID id,
            @RequestParam(required = false) String resolutionNotes,
            @RequestParam(required = false) BigDecimal finalCost) {
        return ResponseEntity.ok(maintenanceTaskService.completeTask(id, resolutionNotes, finalCost));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<MaintenanceTaskResponse>> getTasksByEquipment(@PathVariable UUID equipmentId) {
        return ResponseEntity.ok(maintenanceTaskService.getTasksByEquipment(equipmentId));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceTaskResponse>> getAllTasks(
            @RequestParam(required = false) MaintenanceStatus status) {
        if (status != null) {
            return ResponseEntity.ok(maintenanceTaskService.getTasksByStatus(status));
        }
        return ResponseEntity.ok(maintenanceTaskService.getAllTasks());
    }

    @GetMapping("/technician/{technicianId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('LAB_MANAGER') or hasAuthority('DEPT_HEAD') or #technicianId.toString() == authentication.principal.id.toString()")
    public ResponseEntity<List<MaintenanceTaskResponse>> getTasksByTechnician(@PathVariable UUID technicianId) {
        return ResponseEntity.ok(maintenanceTaskService.getTasksByTechnician(technicianId));
    }
}