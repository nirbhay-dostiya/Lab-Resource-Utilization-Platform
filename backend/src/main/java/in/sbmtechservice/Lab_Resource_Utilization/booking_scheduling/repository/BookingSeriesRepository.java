package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.BookingSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingSeriesRepository extends JpaRepository<BookingSeries, UUID> {
    // Standard CRUD is sufficient here. Most queries will hit the individual Bookings table.
}
