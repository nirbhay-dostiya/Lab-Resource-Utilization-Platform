package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    // Serial numbers are unique, so returning Optional is perfect here
    Optional<Equipment> findBySerialNumber(String serialNumber);

    // Check for duplicate serial numbers during registration
    boolean existsBySerialNumber(String serialNumber);

    // Fetch all equipment belonging to a specific department
    List<Equipment> findByDepartmentId(UUID departmentId);

    // Fetch all equipment belonging to an institution
    @Query("SELECT e FROM Equipment e WHERE e.department.institution.id = :institutionId")
    List<Equipment> findByDepartmentInstitutionId(@Param("institutionId") UUID institutionId);

    // Fetch all equipment belonging to a specific category
    List<Equipment> findByCategoryId(UUID categoryId);

    // Fetch equipment based on its current operational status (e.g., finding all 'AVAILABLE' equipment)
    List<Equipment> findByStatus(EquipmentStatus status);

    // Search for equipment by name (case-insensitive for better UX)
    List<Equipment> findByNameContainingIgnoreCase(String name);

    // Custom query to fetch equipment with its associated tags initialized to prevent LazyInitializationException
    @Query("SELECT e FROM Equipment e LEFT JOIN FETCH e.tags WHERE e.id = :id")
    Optional<Equipment> findByIdWithTags(@Param("id") UUID id);

    // Analytics: Global counts
    long countByStatus(EquipmentStatus status);

    // Analytics: Institution counts
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.department.institution.id = :institutionId")
    long countByDepartmentInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.department.institution.id = :institutionId AND e.status = :status")
    long countByDepartmentInstitutionIdAndStatus(@Param("institutionId") UUID institutionId, @Param("status") EquipmentStatus status);

    // Analytics: Department counts
    long countByDepartmentId(UUID departmentId);

    long countByDepartmentIdAndStatus(UUID departmentId, EquipmentStatus status);

    // Analytics Paginated Queries: Global
    Page<Equipment> findByStatus(EquipmentStatus status, Pageable pageable);
    @Query("SELECT e FROM Equipment e")
    Page<Equipment> findAllEquipment(Pageable pageable);

    // Analytics Paginated Queries: Institution
    @Query("SELECT e FROM Equipment e WHERE e.department.institution.id = :institutionId")
    Page<Equipment> findByDepartmentInstitutionId(@Param("institutionId") UUID institutionId, Pageable pageable);

    @Query("SELECT e FROM Equipment e WHERE e.department.institution.id = :institutionId AND e.status = :status")
    Page<Equipment> findByDepartmentInstitutionIdAndStatus(@Param("institutionId") UUID institutionId, @Param("status") EquipmentStatus status, Pageable pageable);

    // Analytics Paginated Queries: Department
    Page<Equipment> findByDepartmentId(UUID departmentId, Pageable pageable);

    Page<Equipment> findByDepartmentIdAndStatus(UUID departmentId, EquipmentStatus status, Pageable pageable);


}