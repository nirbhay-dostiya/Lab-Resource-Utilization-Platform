package in.sbmtechservice.Lab_Resource_Utilization.auth_user.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AuthenticationRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AuthenticationResponse;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.RegisterRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.RoleRepository;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        // 2. Fetch default role (e.g., RESEARCHER)
        Role defaultRole = roleRepository.findByName(RoleType.RESEARCHER)
                .orElseThrow(() -> new IllegalStateException("Default role not found in database."));

        // 3. Build the User entity
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();

        user.getRoles().add(defaultRole);

        // 4. Save to DB
        userRepository.save(user);

        // 5. Generate JWT Token
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("User registered successfully")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // for authentication
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // for check the credentials are correct or not
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow();

        // token generate
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Authentication successful")
                .build();
    }
}