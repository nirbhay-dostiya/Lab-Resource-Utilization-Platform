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
import in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public String assignRoleToUser(String adminEmail, UUID userId, RoleType roleType, UUID institutionId) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);
        if (!isSystemAdmin && !isInstAdmin) throw new SecurityException("Unauthorized");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = resolveInstitutionId(adminUser);
            UUID targetInstId = resolveInstitutionId(user);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only assign roles to users in your own institution");
            }
        }

        if (roleType == RoleType.SYSTEM_ADMIN) {
            throw new SecurityException("Cannot assign SYSTEM_ADMIN role to existing or new users");
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleType));

        user.getRoles().add(role);

        if (institutionId != null) {
            Institution inst = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalArgumentException("Institution not found with ID: " + institutionId));
            user.setInstitution(inst);
        }

        userRepository.save(user);

        // Notify the affected user + institution admins about role assignment
        UUID targetInstId = institutionId != null ? institutionId : resolveInstitutionId(user);
        String adminName = adminUser.getFirstName() + " " + adminUser.getLastName();
        String userName = user.getFirstName() + " " + user.getLastName();
        eventPublisher.publishEvent(new NotificationEvents.UserRoleAssignedEvent(
                targetInstId, user.getId(), userName, roleType.name(), adminUser.getId(), adminName
        ));

        return "Successfully assigned " + roleType.name() + " role to user: " + user.getEmail();
    }

    @Transactional
    public String removeRoleFromUser(String adminEmail, UUID userId, RoleType roleType) {
        User adminUser = userRepository.findByEmailWithRoles(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean isSystemAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = adminUser.getRoles().stream().anyMatch(r -> r.getName() == RoleType.INSTITUTION_ADMIN);
        if (!isSystemAdmin && !isInstAdmin) throw new SecurityException("Unauthorized");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = resolveInstitutionId(adminUser);
            UUID targetInstId = resolveInstitutionId(user);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only remove roles from users in your own institution");
            }
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleType));

        if (!user.getRoles().contains(role)) {
            throw new IllegalArgumentException("User does not have the role: " + roleType);
        }
        user.getRoles().remove(role);
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
            UUID adminInstId = resolveInstitutionId(adminUser);
            UUID targetInstId = resolveInstitutionId(user);
            if (adminInstId == null || !adminInstId.equals(targetInstId)) {
                throw new SecurityException("You can only change status of users in your own institution");
            }
        }

        boolean newStatus = !user.getIsActive();
        user.setIsActive(newStatus);
        userRepository.save(user);

        // Notify the affected user + institution admins about status change
        UUID targetInstId = resolveInstitutionId(user);
        String adminName = adminUser.getFirstName() + " " + adminUser.getLastName();
        String userName = user.getFirstName() + " " + user.getLastName();
        eventPublisher.publishEvent(new NotificationEvents.UserStatusToggledEvent(
                targetInstId, user.getId(), userName, newStatus, adminUser.getId(), adminName
        ));

        return newStatus ? "User activated successfully." : "User suspended successfully.";
    }

    @Transactional
    public String updateUser(String adminEmail, UUID userId,
                             in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto.UpdateUserRequest request) {
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

        if (isSystemAdmin && isTargetSystemAdmin) {
            if (!adminEmail.equals(userToUpdate.getEmail())) {
                throw new SecurityException("You can only edit your own details, not other System Admins.");
            }
        }

        if (!isSystemAdmin && isTargetSystemAdmin) {
            throw new SecurityException("You cannot edit a System Admin.");
        }

        if (!isSystemAdmin && isInstAdmin) {
            UUID adminInstId = resolveInstitutionId(adminUser);
            UUID targetInstId = resolveInstitutionId(userToUpdate);
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

        // PROFILE UPDATED — self-only notification.
        // Constraint: ONLY the user whose profile changed is notified.
        // No admin, no institution-wide broadcast.
        String updatedUserName = userToUpdate.getFirstName() + " " + userToUpdate.getLastName();
        eventPublisher.publishEvent(new NotificationEvents.ProfileUpdatedEvent(
                userToUpdate.getId(), updatedUserName
        ));

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
            UUID instId = resolveInstitutionId(adminUser);
            if (instId == null) {
                users = new java.util.ArrayList<>();
            } else {
                users = userRepository.findAll().stream().filter(u -> {
                    UUID uInstId = resolveInstitutionId(u);
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
                        .institutionId(resolveInstitutionId(user))
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

            if (!isSystemAdmin) {
                UUID adminInstId = resolveInstitutionId(adminUser);
                if (adminInstId == null || !adminInstId.equals(institution.getId())) {
                    throw new IllegalArgumentException("You can only create users for your own institution's departments");
                }
            }
        } else if (!isSystemAdmin) {
            institution = adminUser.getInstitution() != null ? adminUser.getInstitution()
                    : (adminUser.getDepartment() != null ? adminUser.getDepartment().getInstitution() : null);
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

        // Notify: welcome the new user + notify institution admins
        UUID institutionId = institution != null ? institution.getId() : null;
        String adminName = adminUser.getFirstName() + " " + adminUser.getLastName();
        String newUserName = request.getFirstName() + " " + request.getLastName();
        eventPublisher.publishEvent(new NotificationEvents.UserCreatedEvent(
                institutionId, newUser.getId(), newUserName, request.getEmail(),
                adminUser.getId(), adminName
        ));

        return "User created successfully with role: " + request.getRoleType();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolve the effective institution ID for a user:
     *  1. Direct institution link
     *  2. Department → institution link
     */
    private UUID resolveInstitutionId(User user) {
        if (user.getInstitution() != null) return user.getInstitution().getId();
        if (user.getDepartment() != null && user.getDepartment().getInstitution() != null) {
            return user.getDepartment().getInstitution().getId();
        }
        return null;
    }
}