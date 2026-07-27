package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.controller;

import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto.AccessRequestDto;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto.SharedEquipmentListingDto;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.enums.AccessRequestStatus;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.service.ResourceSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resource-sharing")
@RequiredArgsConstructor
public class ResourceSharingController {

    private final ResourceSharingService resourceSharingService;

    @PostMapping("/listings")
    public ResponseEntity<SharedEquipmentListingDto> createListing(@RequestBody SharedEquipmentListingDto dto) {
        return ResponseEntity.ok(resourceSharingService.createListing(dto));
    }

    @GetMapping("/listings/active")
    public ResponseEntity<List<SharedEquipmentListingDto>> getActiveListings() {
        return ResponseEntity.ok(resourceSharingService.getAllActiveListings());
    }

    @PostMapping("/requests")
    public ResponseEntity<AccessRequestDto> createAccessRequest(@RequestBody AccessRequestDto dto) {
        return ResponseEntity.ok(resourceSharingService.createAccessRequest(dto));
    }

    @PutMapping("/requests/{requestId}/status")
    public ResponseEntity<AccessRequestDto> updateAccessRequestStatus(
            @PathVariable UUID requestId,
            @RequestParam AccessRequestStatus status,
            org.springframework.security.core.Authentication authentication) {
        
        in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails userDetails = 
            (in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails) authentication.getPrincipal();
        UUID approverId = userDetails.getUser().getId();
        
        return ResponseEntity.ok(resourceSharingService.updateAccessRequestStatus(requestId, status, approverId));
    }
    
    @GetMapping("/requests/requester/{requesterId}")
    public ResponseEntity<List<AccessRequestDto>> getRequestsByRequester(@PathVariable UUID requesterId) {
        return ResponseEntity.ok(resourceSharingService.getRequestsByRequester(requesterId));
    }

    @GetMapping("/requests/institution/{institutionId}")
    public ResponseEntity<List<AccessRequestDto>> getRequestsByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(resourceSharingService.getRequestsByInstitution(institutionId));
    }
}
