package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums;

/**
 * Priority level for Work Orders / Maintenance Tasks.
 * Used in state-machine routing and Kanban board urgency coloring.
 */
public enum MaintenancePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
