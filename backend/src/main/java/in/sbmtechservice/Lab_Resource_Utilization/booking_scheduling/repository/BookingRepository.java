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

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Fetch a user's booking history
    List<Booking> findByUserId(UUID userId);

    // Fetch the booking calendar for a specific piece of equipment
    List<Booking> findByEquipmentId(UUID equipmentId);

    // Find all bookings with a specific status (e.g., to clean up 'NO_SHOW's)
    List<Booking> findByStatus(BookingStatus status);

    // 🚨 CORE SCHEDULING LOGIC: Check for overlapping bookings to prevent double-booking 🚨
    @Query("SELECT b FROM Booking b WHERE b.equipment.id = :equipmentId " +
            "AND b.status IN ('CONFIRMED', 'IN_USE') " +
            "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(
            @Param("equipmentId") UUID equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}