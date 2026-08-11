package in.sbmtechservice.Lab_Resource_Utilization.institution.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.DepartmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.DepartmentResponse;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.DepartmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isSystemAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.SYSTEM_ADMIN);

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new IllegalArgumentException("Institution not found."));

        if (!isSystemAdmin) {
            UUID userInstId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (userInstId == null || !userInstId.equals(institution.getId())) {
                throw new SecurityException("You do not have permission to create a department in this institution.");
            }
        }

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(request.getCode())
                .institution(institution)
                .isActive(true)
                .build();

        Department saved = departmentRepository.save(department);

        // Notify institution admins + dept heads of the same institution (creator gets self-confirm)
        UUID institutionId = institution.getId();
        String creatorName = user.getFirstName() + " " + user.getLastName();
        eventPublisher.publishEvent(new NotificationEvents.DepartmentCreatedEvent(
                institutionId, saved.getId(), saved.getName(), user.getId(), creatorName
        ));

        return mapToResponse(saved);
    }

    public List<DepartmentResponse> getDepartmentsByInstitution(UUID institutionId) {
        return departmentRepository.findByInstitutionId(institutionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isSystemAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.SYSTEM_ADMIN);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));

        if (!isSystemAdmin) {
            UUID userInstId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (userInstId == null || !userInstId.equals(department.getInstitution().getId())) {
                throw new SecurityException("You do not have permission to update this department.");
            }
        }

        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());

        Department saved = departmentRepository.save(department);

        // Notify institution admins + dept heads of the same institution
        UUID institutionId = department.getInstitution().getId();
        String updaterName = user.getFirstName() + " " + user.getLastName();
        eventPublisher.publishEvent(new NotificationEvents.DepartmentUpdatedEvent(
                institutionId, saved.getId(), saved.getName(), user.getId(), updaterName
        ));

        return mapToResponse(saved);
    }

    @Transactional
    public String assignUserToDepartment(UUID targetUserId, UUID departmentId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        boolean isSystemAdmin = admin.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.SYSTEM_ADMIN);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));

        if (!isSystemAdmin) {
            UUID adminInstId = admin.getInstitution() != null ? admin.getInstitution().getId() : (admin.getDepartment() != null && admin.getDepartment().getInstitution() != null ? admin.getDepartment().getInstitution().getId() : null);
            if (adminInstId == null || !adminInstId.equals(department.getInstitution().getId())) {
                throw new SecurityException("You do not have permission to assign a user to this institution's department.");
            }
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User to assign not found."));

        targetUser.setDepartment(department);
        userRepository.save(targetUser);

        return "User " + targetUser.getEmail() + " successfully assigned to " + department.getName();
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .institutionId(department.getInstitution().getId())
                .institutionName(department.getInstitution().getName())
                .build();
    }
}