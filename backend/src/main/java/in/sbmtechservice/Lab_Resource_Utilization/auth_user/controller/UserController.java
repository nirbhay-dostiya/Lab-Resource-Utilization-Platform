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

    // Only Admins should be allowed to assign roles to other people!
    @PostMapping("/assign-role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> assignRole(@RequestBody RoleAssignmentRequest request) {
        String result = userService.assignRoleToUser(request.getUserId(), request.getNewRole());
        return ResponseEntity.ok(result);
    }
}