package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Dashboard data for LAB_MANAGER / DEPT_HEAD persona.
 * Shows equipment utilization rates, work order summary, and technician workload.
 */
@Data
@Builder
public class LabManagerDashboardDto {

    /** Total equipment in this institution. */
    private long totalEquipment;

    /** Number of equipment currently under maintenance. */
    private long underMaintenance;

    /** Open work orders by status. Key = status name, Value = count. */
    private Map<String, Long> workOrdersByStatus;

    /** Top 5 most-booked equipment in last 30 days. Key = name, Value = usage hours. */
    private Map<String, Double> topEquipmentByUsageHours;

    /** Utilization rate list per equipment (last 30 days). */
    private List<EquipmentUtilization> utilizationRates;

    /** Total confirmed bookings this month. */
    private long bookingsThisMonth;

    @Data
    @Builder
    public static class EquipmentUtilization {
        private String equipmentId;
        private String equipmentName;
        /** Usage hours in the last 30 days. */
        private double usageHours;
        /** Utilization rate as % (usageHours / max available hours in period * 100). */
        private double utilizationPct;
    }
}
