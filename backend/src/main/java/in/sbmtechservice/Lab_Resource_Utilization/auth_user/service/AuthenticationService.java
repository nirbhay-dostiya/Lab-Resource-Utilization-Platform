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

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.InstitutionRegisterRequest;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.StudentRegisterRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;
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

    @Transactional
    public AuthenticationResponse registerInstitution(InstitutionRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }
        if (institutionRepository.existsByName(request.getInstitutionName()) || institutionRepository.existsByDomain(request.getDomain())) {
            throw new IllegalArgumentException("Institution name or domain already exists.");
        }

        // 1. Create Institution
        Institution institution = Institution.builder()
                .name(request.getInstitutionName())
                .domain(request.getDomain())
                .address(request.getAddress())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getEmail())
                .isActive(false) // Pending verification
                .build();
        institution = institutionRepository.save(institution);

        // 2. Fetch INSTITUTION_ADMIN role
        Role adminRole = roleRepository.findByName(RoleType.INSTITUTION_ADMIN)
                .orElseThrow(() -> new IllegalStateException("INSTITUTION_ADMIN role not found."));

        // 3. Create Admin User
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(false) // Pending verification
                .institution(institution)
                .build();
        user.getRoles().add(adminRole);
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .token("")
                .message("Institution registered successfully and is pending verification by System Admin.")
                .build();
    }

    @Transactional
    public AuthenticationResponse registerStudent(StudentRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new IllegalArgumentException("Institution not found."));

        Role studentRole = roleRepository.findByName(RoleType.STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found."));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .institution(institution)
                .build();
        user.getRoles().add(studentRole);
        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Student registered successfully")
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