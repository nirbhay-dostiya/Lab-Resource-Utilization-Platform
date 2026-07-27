package in.sbmtechservice.Lab_Resource_Utilization.institution.repository;

import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.UserDepartment;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.UserDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, UserDepartmentId> {

    // Find all departments a specific user belongs to
    List<UserDepartment> findByUserId(UUID userId);

    // Find all users belonging to a specific department
    List<UserDepartment> findByDepartmentId(UUID departmentId);

    // Find the primary department for a specific user
    Optional<UserDepartment> findByUserIdAndIsPrimaryTrue(UUID userId);

    // Custom query to fetch members of a department along with their User entity data (to avoid N+1 problem)
    @Query("SELECT ud FROM UserDepartment ud JOIN FETCH ud.user WHERE ud.department.id = :departmentId")
    List<UserDepartment> findMembershipsWithUsersByDepartmentId(@Param("departmentId") UUID departmentId);
}