package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Fetch a user's booking history
    List<Booking> findByUserId(UUID userId);

    // Fetch the booking calendar for a specific piece of equipment
    List<Booking> findByEquipmentId(UUID equipmentId);

    // Find all bookings with a specific status (e.g., to clean up 'NO_SHOW's)
    List<Booking> findByStatus(BookingStatus status);

    // Fetch bookings by Institution
    @Query("SELECT b FROM Booking b WHERE b.equipment.department.institution.id = :institutionId")
    List<Booking> findByEquipmentDepartmentInstitutionId(@Param("institutionId") UUID institutionId);

    // Fetch bookings by Department
    @Query("SELECT b FROM Booking b WHERE b.equipment.department.id = :departmentId")
    List<Booking> findByEquipmentDepartmentId(@Param("departmentId") UUID departmentId);

    // 🚨 CORE SCHEDULING LOGIC: Check for overlapping bookings to prevent double-booking 🚨
    @Query("SELECT b FROM Booking b WHERE b.equipment.id = :equipmentId " +
            "AND b.status IN ('CONFIRMED', 'IN_USE') " +
            "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(
            @Param("equipmentId") UUID equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Analytics: Global counts
    long countByStatus(BookingStatus status);

    @Query("SELECT b.equipment.name as equipmentName, COUNT(b) as count FROM Booking b GROUP BY b.equipment.name")
    List<Object[]> countBookingsByEquipmentGlobal();

    // Analytics: Institution counts
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.equipment.department.institution.id = :institutionId")
    long countByEquipmentDepartmentInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.equipment.department.institution.id = :institutionId AND b.status = :status")
    long countByEquipmentDepartmentInstitutionIdAndStatus(@Param("institutionId") UUID institutionId, @Param("status") BookingStatus status);

    @Query("SELECT b.equipment.name as equipmentName, COUNT(b) as count FROM Booking b WHERE b.equipment.department.institution.id = :institutionId GROUP BY b.equipment.name")
    List<Object[]> countBookingsByEquipmentForInstitution(@Param("institutionId") UUID institutionId);

    // Analytics: Department counts
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.equipment.department.id = :departmentId")
    long countByEquipmentDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.equipment.department.id = :departmentId AND b.status = :status")
    long countByEquipmentDepartmentIdAndStatus(@Param("departmentId") UUID departmentId, @Param("status") BookingStatus status);

    @Query("SELECT b.equipment.name as equipmentName, COUNT(b) as count FROM Booking b WHERE b.equipment.department.id = :departmentId GROUP BY b.equipment.name")
    List<Object[]> countBookingsByEquipmentForDepartment(@Param("departmentId") UUID departmentId);

    // Analytics Paginated Queries: Global
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    @Query("SELECT b FROM Booking b")
    Page<Booking> findAllBookings(Pageable pageable);

    // Analytics Paginated Queries: Institution
    @Query("SELECT b FROM Booking b WHERE b.equipment.department.institution.id = :institutionId")
    Page<Booking> findByEquipmentDepartmentInstitutionId(@Param("institutionId") UUID institutionId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.equipment.department.institution.id = :institutionId AND b.status = :status")
    Page<Booking> findByEquipmentDepartmentInstitutionIdAndStatus(@Param("institutionId") UUID institutionId, @Param("status") BookingStatus status, Pageable pageable);

    // Analytics Paginated Queries: Department
    @Query("SELECT b FROM Booking b WHERE b.equipment.department.id = :departmentId")
    Page<Booking> findByEquipmentDepartmentId(@Param("departmentId") UUID departmentId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.equipment.department.id = :departmentId AND b.status = :status")
    Page<Booking> findByEquipmentDepartmentIdAndStatus(@Param("departmentId") UUID departmentId, @Param("status") BookingStatus status, Pageable pageable);

}