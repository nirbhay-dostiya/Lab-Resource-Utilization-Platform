package in.sbmtechservice.Lab_Resource_Utilization.institution.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.DepartmentRequest;
import in.sbmtechservice.Lab_Resource_Utilization.institution.dto.DepartmentResponse;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.DepartmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new IllegalArgumentException("Institution not found."));

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(request.getCode())
                .institution(institution)
                .isActive(true)
                .build();

        Department saved = departmentRepository.save(department);
        return mapToResponse(saved);
    }

    public List<DepartmentResponse> getDepartmentsByInstitution(UUID institutionId) {
        return departmentRepository.findByInstitutionId(institutionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String assignUserToDepartment(UUID userId, UUID departmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));

        user.setDepartment(department);
        userRepository.save(user);

        return "User " + user.getEmail() + " successfully assigned to " + department.getName();
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