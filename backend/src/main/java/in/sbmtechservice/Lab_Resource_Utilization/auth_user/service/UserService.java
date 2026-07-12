package in.sbmtechservice.Lab_Resource_Utilization.auth_user.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.RoleRepository;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public String assignRoleToUser(UUID userId, RoleType roleType) {
        // 1. Find the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 2. Find the Role
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleType));

        // 3. Add the role to the user (Because it is a Set, duplicates are automatically ignored)
        user.getRoles().add(role);

        // 4. Save the user
        userRepository.save(user);

        return "Successfully assigned " + roleType.name() + " role to user: " + user.getEmail();
    }

    public java.util.List<in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isActive(user.getIsActive())
                        .roles(user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .toList())
                        .build())
                .toList();
    }
}