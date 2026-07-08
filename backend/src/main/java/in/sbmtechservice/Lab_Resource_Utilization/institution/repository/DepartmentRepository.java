package in.sbmtechservice.Lab_Resource_Utilization.institution.repository;

import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    // Fetch all departments belonging to a specific institution
    List<Department> findByInstitutionId(UUID institutionId);

    // Validate if a department code is unique within a specific institution
    boolean existsByInstitutionIdAndCode(UUID institutionId, String code);

    // Fetch a specific department by its institution and code
    Optional<Department> findByInstitutionIdAndCode(UUID institutionId, String code);
}