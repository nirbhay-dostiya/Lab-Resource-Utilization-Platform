package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, UUID> {

    // Retrieve the audit trail of a booking from creation to completion/cancellation
    List<BookingStatusHistory> findByBookingIdOrderByChangedAtDesc(UUID bookingId);
}