package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dashboard data for SYSTEM_ADMIN persona.
 * Shows cross-institution financial health, TCO analysis, and system bottlenecks.
 */
@Data
@Builder
public class SystemAdminDashboardDto {

    /** Total institutions registered in the system. */
    private long totalInstitutions;

    /** Total equipment across all institutions. */
    private long totalEquipment;

    /** Total users in the system. */
    private long totalUsers;

    /** Total invoiced amount (all time). */
    private BigDecimal totalInvoiced;

    /** Total paid amount. */
    private BigDecimal totalPaid;

    /** Total overdue amount. */
    private BigDecimal totalOverdue;

    /** Open work orders count. */
    private long openWorkOrders;

    /** Top 5 most-booked (bottleneck) equipment. Key = name, Value = booking count. */
    private Map<String, Long> topBookedEquipment;

    /** Platform-wide booking count today. */
    private long bookingsToday;

    /** Equipment under maintenance today. */
    private long underMaintenanceCount;

    /** Institutions with most pending approvals (up to 5). */
    private List<InstitutionApprovalLoad> pendingApprovalByInstitution;

    @Data
    @Builder
    public static class InstitutionApprovalLoad {
        private String institutionName;
        private long pendingCount;
    }
}
