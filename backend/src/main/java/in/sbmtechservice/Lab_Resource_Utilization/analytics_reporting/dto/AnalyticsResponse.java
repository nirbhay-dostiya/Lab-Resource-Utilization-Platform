package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsResponse {
    private long totalEquipment;
    private long underMaintenance;
    private long totalBookings;
    private long pendingApprovals;
    private java.util.Map<String, Long> bookingsByEquipment;
}
