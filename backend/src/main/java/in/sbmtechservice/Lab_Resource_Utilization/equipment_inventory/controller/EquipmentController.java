package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.controller;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.EquipmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.EquipmentResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<EquipmentResponse> addEquipment(
            @RequestBody EquipmentRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(equipmentService.addEquipment(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipmentResponse>> getAllEquipment() {
        return ResponseEntity.ok(equipmentService.getAllEquipment());
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(equipmentService.getEquipmentByInstitution(institutionId));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("isAuthenticated()") // Anyone logged in can see what equipment exists
    public ResponseEntity<List<EquipmentResponse>> getEquipmentByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(equipmentService.getEquipmentByDepartment(departmentId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER') or hasAuthority('LAB_TECHNICIAN') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<EquipmentResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam EquipmentStatus status,
            Principal principal
    ) {
        return ResponseEntity.ok(equipmentService.updateEquipmentStatus(id, status, principal.getName()));
    }
}