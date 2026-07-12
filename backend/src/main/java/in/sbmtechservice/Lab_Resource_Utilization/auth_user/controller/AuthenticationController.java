package in.sbmtechservice.Lab_Resource_Utilization.auth_user.controller;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AuthenticationRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AuthenticationResponse;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.RegisterRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.service.AuthenticationService;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserProfile(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails) {
            in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails userDetails = (in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails) authentication.getPrincipal();
            in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User user = userDetails.getUser();
            user = userRepository.findById(user.getId()).orElse(user);
            
            in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UserResponse response = in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .isActive(user.getIsActive())
                    .roles(user.getRoles().stream().map(r -> r.getName().name()).toList())
                    .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                    .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                    .institutionId(user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null))
                    .institutionName(user.getInstitution() != null ? user.getInstitution().getName() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getName() : null))
                    .build();
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Unauthorized");
    }
}