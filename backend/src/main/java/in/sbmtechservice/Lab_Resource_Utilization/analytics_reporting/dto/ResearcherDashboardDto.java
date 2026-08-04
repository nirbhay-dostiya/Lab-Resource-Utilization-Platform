package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard data for RESEARCHER persona.
 * Shows personal usage, upcoming reservations, and spending vs. budget.
 */
@Data
@Builder
public class ResearcherDashboardDto {

    /** Total bookings this calendar month by this researcher. */
    private long bookingsThisMonth;

    /** Total spend (₹) this month across all their bookings. */
    private BigDecimal spendThisMonth;

    /** Upcoming confirmed/pending bookings (next 30 days). */
    private List<UpcomingBooking> upcomingBookings;

    @Data
    @Builder
    public static class UpcomingBooking {
        private UUID bookingId;
        private String equipmentName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }
}
