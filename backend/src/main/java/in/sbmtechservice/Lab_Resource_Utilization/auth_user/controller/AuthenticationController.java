package in.sbmtechservice.Lab_Resource_Utilization.auth_user.controller;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AuthenticationRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AuthenticationResponse;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.RegisterRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

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
    public ResponseEntity<?> getUserProfile(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ResponseEntity.ok(authentication.getPrincipal());
        }
        return ResponseEntity.status(401).body("Unauthorized");
    }
}