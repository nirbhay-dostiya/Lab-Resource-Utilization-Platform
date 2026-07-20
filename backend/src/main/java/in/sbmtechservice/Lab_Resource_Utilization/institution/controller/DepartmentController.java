package in.sbmtechservice.Lab_Resource_Utilization.institution.controller;

import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.AssignDepartmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.DepartmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.DepartmentResponse;
import in.sbmtechservice.Lab_Resource_Utilization.institution.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentRequest request, java.security.Principal principal) {
        return ResponseEntity.ok(departmentService.createDepartment(request, principal.getName()));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(departmentService.getDepartmentsByInstitution(institutionId));
    }

    @PostMapping("/assign-user")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<String> assignUserToDepartment(@RequestBody AssignDepartmentRequest request, java.security.Principal principal) {
        String response = departmentService.assignUserToDepartment(request.getUserId(), request.getDepartmentId(), principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable UUID id, @RequestBody DepartmentRequest request, java.security.Principal principal) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request, principal.getName()));
    }
}