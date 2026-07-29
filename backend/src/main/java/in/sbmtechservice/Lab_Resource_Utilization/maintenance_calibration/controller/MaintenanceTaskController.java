package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.controller;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service.MaintenanceTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceTaskController {

    private final MaintenanceTaskService maintenanceTaskService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<MaintenanceTaskResponse> scheduleTask(@RequestBody MaintenanceTaskRequest request) {
        return ResponseEntity.ok(maintenanceTaskService.scheduleTask(request));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_TECHNICIAN')")
    public ResponseEntity<MaintenanceTaskResponse> completeTask(
            @PathVariable UUID id,
            @RequestParam(required = true) String resolutionNotes,
            @RequestParam(required = false) BigDecimal finalCost
    ) {
        return ResponseEntity.ok(maintenanceTaskService.completeTask(id, resolutionNotes, finalCost));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<java.util.List<MaintenanceTaskResponse>> getTasksByEquipment(@PathVariable UUID equipmentId) {
        return ResponseEntity.ok(maintenanceTaskService.getTasksByEquipment(equipmentId));
    }

    @GetMapping
    public ResponseEntity<java.util.List<MaintenanceTaskResponse>> getAllTasks() {
        return ResponseEntity.ok(maintenanceTaskService.getAllTasks());
    }
}