package in.sbmtechservice.Lab_Resource_Utilization.auth_user.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.RoleRepository;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.DepartmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.AdminCreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String assignRoleToUser(String adminEmail, UUID userId, RoleType roleType, UUID institutionId) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);
        if (!isSystemAdmin && !isInstAdmin) throw new SecurityException("Unauthorized");

        // 1. Find the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = adminUser.getInstitution() != null ? adminUser.getInstitution().getId() : (adminUser.getDepartment() != null && adminUser.getDepartment().getInstitution() != null ? adminUser.getDepartment().getInstitution().getId() : null);
            UUID targetInstId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only assign roles to users in your own institution");
            }
        }

        if (roleType == RoleType.SYSTEM_ADMIN) {
            throw new SecurityException("Cannot assign SYSTEM_ADMIN role to existing or new users");
        }

        // 2. Find the Role
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleType));

        // 3. Add the role to the user (Because it is a Set, duplicates are automatically ignored)
        user.getRoles().add(role);

        if (institutionId != null) {
            Institution inst = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalArgumentException("Institution not found with ID: " + institutionId));
            user.setInstitution(inst);
        }

        // 4. Save the user
        userRepository.save(user);

        return "Successfully assigned " + roleType.name() + " role to user: " + user.getEmail();
    }

    @Transactional
    public String removeRoleFromUser(String adminEmail, UUID userId, RoleType roleType) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);
        if (!isSystemAdmin && !isInstAdmin) throw new SecurityException("Unauthorized");

        // 1. Find the User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = adminUser.getInstitution() != null ? adminUser.getInstitution().getId() : (adminUser.getDepartment() != null && adminUser.getDepartment().getInstitution() != null ? adminUser.getDepartment().getInstitution().getId() : null);
            UUID targetInstId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only remove roles from users in your own institution");
            }
        }

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

    @Transactional
    public String toggleUserStatus(String adminEmail, UUID userId) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);
        if (!isSystemAdmin && !isInstAdmin) throw new SecurityException("Unauthorized");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = adminUser.getInstitution() != null ? adminUser.getInstitution().getId() : (adminUser.getDepartment() != null && adminUser.getDepartment().getInstitution() != null ? adminUser.getDepartment().getInstitution().getId() : null);
            UUID targetInstId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only change status of users in your own institution");
            }
        }

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        return user.getIsActive() ? "User activated successfully." : "User suspended successfully.";
    }


    @Transactional
    public String updateUser(String adminEmail, UUID userId, in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UpdateUserRequest request) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);

        if (!isSystemAdmin && !isInstAdmin) {
            throw new SecurityException("You do not have permission to edit users.");
        }

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isTargetSystemAdmin = userToUpdate.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);

        // System Admins cannot edit other System Admins
        if (isSystemAdmin && isTargetSystemAdmin) {
            if (!adminEmail.equals(userToUpdate.getEmail())) {
                throw new SecurityException("You can only edit your own details, not other System Admins.");
            }
        }

        // Institution Admin cannot edit System Admin
        if (!isSystemAdmin && isTargetSystemAdmin) {
            throw new SecurityException("You cannot edit a System Admin.");
        }

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = adminUser.getInstitution() != null ? adminUser.getInstitution().getId() : (adminUser.getDepartment() != null && adminUser.getDepartment().getInstitution() != null ? adminUser.getDepartment().getInstitution().getId() : null);
            UUID targetInstId = userToUpdate.getInstitution() != null ? userToUpdate.getInstitution().getId() : (userToUpdate.getDepartment() != null && userToUpdate.getDepartment().getInstitution() != null ? userToUpdate.getDepartment().getInstitution().getId() : null);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only edit users in your own institution");
            }
        }

        userToUpdate.setFirstName(request.getFirstName());
        userToUpdate.setLastName(request.getLastName());
        
        if (!userToUpdate.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email is already in use.");
            }
            userToUpdate.setEmail(request.getEmail());
        }

        userRepository.save(userToUpdate);
        return "User updated successfully.";
    }

    public java.util.List<in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UserResponse> getAllUsers(String adminEmail) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);

        if (!isSystemAdmin && !isInstAdmin) {
            throw new SecurityException("You do not have permission to view users.");
        }

        java.util.List<User> users;
        if (isSystemAdmin) {
            users = userRepository.findAll();
        } else {
            UUID instId = adminUser.getInstitution() != null ? adminUser.getInstitution().getId() : (adminUser.getDepartment() != null && adminUser.getDepartment().getInstitution() != null ? adminUser.getDepartment().getInstitution().getId() : null);
            if (instId == null) {
                users = new java.util.ArrayList<>();
            } else {
                users = userRepository.findAll().stream().filter(u -> {
                    UUID uInstId = u.getInstitution() != null ? u.getInstitution().getId() : (u.getDepartment() != null && u.getDepartment().getInstitution() != null ? u.getDepartment().getInstitution().getId() : null);
                    return instId.equals(uInstId);
                }).toList();
            }
        }

        return users.stream()
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

    @Transactional
    public String changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "Password successfully changed.";
    }

    @Transactional
    public String adminCreateUser(String adminEmail, AdminCreateUserRequest request) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstitutionAdmin = adminUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);

        if (!isSystemAdmin && !isInstitutionAdmin) {
            throw new IllegalArgumentException("You do not have permission to create users");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        Department department = null;
        Institution institution = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            institution = department.getInstitution();
            
            // Check if INSTITUTION_ADMIN is creating user for their own institution
            if (!isSystemAdmin) {
                UUID adminInstId = adminUser.getInstitution() != null ? adminUser.getInstitution().getId() : (adminUser.getDepartment() != null && adminUser.getDepartment().getInstitution() != null ? adminUser.getDepartment().getInstitution().getId() : null);
                if (adminInstId == null || !adminInstId.equals(institution.getId())) {
                    throw new IllegalArgumentException("You can only create users for your own institution's departments");
                }
            }
        } else if (!isSystemAdmin) {
            // For institution admins, if they don't specify a department, assign the user to their own institution
            institution = adminUser.getInstitution() != null ? adminUser.getInstitution() : (adminUser.getDepartment() != null ? adminUser.getDepartment().getInstitution() : null);
        }

        if (request.getRoleType() == RoleType.SYSTEM_ADMIN) {
            throw new SecurityException("Cannot create a user with SYSTEM_ADMIN role");
        }

        Role assignedRole = roleRepository.findByName(request.getRoleType())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .department(department)
                .institution(institution)
                .build();

        newUser.getRoles().add(assignedRole);
        userRepository.save(newUser);

        return "User created successfully with role: " + request.getRoleType();
    }
}