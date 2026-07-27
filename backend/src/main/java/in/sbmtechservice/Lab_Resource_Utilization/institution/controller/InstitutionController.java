package in.sbmtechservice.Lab_Resource_Utilization.institution.controller;

import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.InstitutionRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.InstitutionResponse;
import in.sbmtechservice.Lab_Resource_Utilization.institution.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<InstitutionResponse> createInstitution(@RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(institutionService.createInstitution(request));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<InstitutionResponse> verifyInstitution(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(institutionService.verifyInstitution(id));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<InstitutionResponse> suspendInstitution(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(institutionService.suspendInstitution(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()") // Anyone logged in can view the list of institutions
    public ResponseEntity<List<InstitutionResponse>> getAllInstitutions(java.security.Principal principal) {
        return ResponseEntity.ok(institutionService.getAllInstitutions(principal.getName()));
    }

    @GetMapping("/public")
    public ResponseEntity<List<InstitutionResponse>> getPublicInstitutions() {
        return ResponseEntity.ok(institutionService.getPublicInstitutions());
    }
}