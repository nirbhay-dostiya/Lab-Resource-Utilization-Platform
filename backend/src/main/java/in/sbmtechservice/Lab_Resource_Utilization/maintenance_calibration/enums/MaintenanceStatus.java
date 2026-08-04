package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums;

/**
 * Full 6-state Work Order lifecycle enum.
 * Transitions are enforced by the MaintenanceTaskService state machine.
 *
 * CREATED      → ASSIGNED   (LAB_MANAGER, DEPT_HEAD, SYSTEM_ADMIN)
 * ASSIGNED     → IN_PROGRESS (LAB_TECHNICIAN, LAB_MANAGER)
 * IN_PROGRESS  → COMPLETED  (LAB_TECHNICIAN, LAB_MANAGER)
 * COMPLETED    → VERIFIED   (LAB_MANAGER, DEPT_HEAD, SYSTEM_ADMIN — final gate)
 * *            → CANCELLED  (LAB_MANAGER, SYSTEM_ADMIN)
 */
public enum MaintenanceStatus {
    CREATED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    VERIFIED,
    CANCELLED
}
