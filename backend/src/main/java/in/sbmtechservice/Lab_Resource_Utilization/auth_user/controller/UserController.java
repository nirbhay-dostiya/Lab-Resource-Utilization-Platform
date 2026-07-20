package in.sbmtechservice.Lab_Resource_Utilization.auth_user.controller;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.RoleAssignmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/assign-role")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<String> assignRole(@RequestBody RoleAssignmentRequest request, java.security.Principal principal) {
        String result = userService.assignRoleToUser(principal.getName(), request.getUserId(), request.getNewRole(), request.getInstitutionId());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{userId}/roles/{role}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<String> removeRole(
            @PathVariable java.util.UUID userId,
            @PathVariable in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType role,
            java.security.Principal principal
    ) {
        String result = userService.removeRoleFromUser(principal.getName(), userId, role);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{userId}/toggle-status")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable java.util.UUID userId,
            java.security.Principal principal
    ) {
        try {
            String result = userService.toggleUserStatus(principal.getName(), userId);
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }


    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable java.util.UUID userId,
            @RequestBody in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UpdateUserRequest request,
            java.security.Principal principal
    ) {
        try {
            String result = userService.updateUser(principal.getName(), userId, request);
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<java.util.List<in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UserResponse>> getAllUsers(java.security.Principal principal) {
        return ResponseEntity.ok(userService.getAllUsers(principal.getName()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.ChangePasswordRequest request,
            java.security.Principal principal
    ) {
        try {
            String result = userService.changePassword(principal.getName(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin-create")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<?> adminCreateUser(
            @RequestBody in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AdminCreateUserRequest request,
            java.security.Principal principal
    ) {
        try {
            String result = userService.adminCreateUser(principal.getName(), request);
            return ResponseEntity.ok(java.util.Map.of("message", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}