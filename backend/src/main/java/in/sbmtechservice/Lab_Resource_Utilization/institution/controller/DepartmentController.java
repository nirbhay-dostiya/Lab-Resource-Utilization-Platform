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
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.createDepartment(request));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(departmentService.getDepartmentsByInstitution(institutionId));
    }

    @PostMapping("/assign-user")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD')")
    public ResponseEntity<String> assignUserToDepartment(@RequestBody AssignDepartmentRequest request) {
        String response = departmentService.assignUserToDepartment(request.getUserId(), request.getDepartmentId());
        return ResponseEntity.ok(response);
    }
}