package in.sbmtechservice.Lab_Resource_Utilization.auth_user.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.RoleRepository;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;

    @Transactional
    public String assignRoleToUser(UUID userId, RoleType roleType, UUID institutionId) {
        // 1. Find the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 2. Find the Role
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleType));

        // 3. Add the role to the user (Because it is a Set, duplicates are automatically ignored)
        user.getRoles().add(role);

        if (roleType == RoleType.INSTITUTION_ADMIN && institutionId != null) {
            Institution inst = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalArgumentException("Institution not found with ID: " + institutionId));
            user.setInstitution(inst);
        }

        // 4. Save the user
        userRepository.save(user);

        return "Successfully assigned " + roleType.name() + " role to user: " + user.getEmail();
    }

    @Transactional
    public String removeRoleFromUser(UUID userId, RoleType roleType) {
        // 1. Find the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 2. Find the Role
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleType));

        // 3. Remove the role
        if (!user.getRoles().contains(role)) {
             throw new IllegalArgumentException("User does not have the role: " + roleType);
        }
        user.getRoles().remove(role);

        // 4. Save the user
        userRepository.save(user);

        return "Successfully removed " + roleType.name() + " role from user: " + user.getEmail();
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
                        .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                        .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                        .institutionId(user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null))
                        .institutionName(user.getInstitution() != null ? user.getInstitution().getName() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getName() : null))
                        .build())
                .toList();
    }
}