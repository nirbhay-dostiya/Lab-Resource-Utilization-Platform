package in.sbmtechservice.Lab_Resource_Utilization.notification.event;

import java.util.UUID;

/**
 * Domain events for the notification system.
 * Emitted by business services to decouple transaction commits from async notification delivery.
 *
 * Audience constraint enforced at the Dispatcher level:
 *  - PROFILE events → only the affected user
 *  - INSTITUTION/DEPARTMENT events → scoped to that institution only
 *  - CATEGORY events → System Admins only (global entity)
 *  - REPORT events → only the requester
 */
public class NotificationEvents {

    // ── Equipment Events ──────────────────────────────────────────────────────

    public record EquipmentAddedEvent(
            UUID institutionId,
            UUID equipmentId,
            String equipmentName,
            UUID addedById,
            String addedByName
    ) {}

    public record EquipmentUpdatedEvent(
            UUID institutionId,
            UUID equipmentId,
            String equipmentName,
            String updatedByName
    ) {}

    public record EquipmentStatusChangedEvent(
            UUID institutionId,
            UUID equipmentId,
            String equipmentName,
            String oldStatus,
            String newStatus,
            String changedByName
    ) {}

    /** Fired when an asset enters downtime (maintenance scheduled). */
    public record AssetDowntimeEvent(
            UUID institutionId,
            UUID equipmentId,
            String equipmentName,
            String estimatedDowntimeHours
    ) {}

    // ── Booking Events ────────────────────────────────────────────────────────

    public record BookingPendingApprovalEvent(
            UUID equipmentInstitutionId,
            UUID bookingId,
            String bookerName,
            String equipmentName,
            String startTime
    ) {}

    public record BookingInvoiceGeneratedEvent(
            UUID bookerId,
            UUID bookingId,
            String equipmentName,
            String amount
    ) {}

    public record BookingConfirmedEvent(
            UUID bookerId,
            UUID bookingId,
            String equipmentName,
            String startTime
    ) {}

    public record BookingCancelledEvent(
            UUID bookerId,
            UUID equipmentInstitutionId,
            UUID bookingId,
            String equipmentName,
            String cancelledByName
    ) {}

    public record BookingCompletedEvent(
            UUID bookerId,
            UUID bookingId,
            String equipmentName
    ) {}

    public record WaitlistFulfilledEvent(
            UUID userId,
            UUID waitlistId,
            String equipmentName
    ) {}

    // ── Maintenance / Work Order Events ──────────────────────────────────────

    public record MaintenanceScheduledEvent(
            UUID technicianId,
            UUID institutionId,
            UUID taskId,
            String equipmentName,
            String scheduledDate
    ) {}

    public record MaintenanceCompletedEvent(
            UUID institutionId,
            UUID taskId,
            String equipmentName
    ) {}

    /** Fired for HIGH/CRITICAL priority work orders on state transitions. */
    public record WorkOrderUrgentEvent(
            UUID institutionId,
            UUID workOrderId,
            String equipmentName,
            String priority,
            String currentStatus
    ) {}

    /**
     * Fired on every work order state transition.
     * Notifies: assigned technician + Dept Heads + Inst Admins (all scoped to institution).
     */
    public record WorkOrderStatusChangedEvent(
            UUID institutionId,
            UUID technicianId,
            UUID workOrderId,
            String equipmentName,
            String newStatus,
            UUID changedById,
            String changedByName
    ) {}

    // ── Calibration Events ────────────────────────────────────────────────────

    /**
     * Fired by CalibrationReminderScheduler at 30, 14, 7, and 1 day(s) before expiry.
     * daysUntilExpiry distinguishes which reminder stage this is.
     */
    public record CalibrationReminderEvent(
            UUID institutionId,
            UUID calibrationRecordId,
            UUID equipmentId,
            String equipmentName,
            String expiryDate,
            int daysUntilExpiry
    ) {}

    /**
     * Fired when a new calibration record is logged.
     * Notifies: Lab Managers + Dept Heads of the equipment's institution.
     * Actor (loggedBy) gets a self-confirmation.
     */
    public record CalibrationLoggedEvent(
            UUID institutionId,
            UUID calibrationId,
            UUID equipmentId,
            String equipmentName,
            String expiryDate,
            UUID loggedById,
            String loggedByName
    ) {}

    // ── Billing Events ────────────────────────────────────────────────────────

    public record BillingFailedEvent(
            UUID invoiceId,
            UUID departmentId,
            UUID institutionId,
            String reason
    ) {}

    public record InvoiceApprovalRequestedEvent(
            UUID invoiceId,
            UUID institutionId,
            String invoiceAmount
    ) {}

    public record InvoiceApprovedEvent(
            UUID invoiceId,
            UUID issuedToDepartmentId,
            String invoiceAmount
    ) {}

    // ── Resource Sharing Events ───────────────────────────────────────────────

    public record ResourceShareListedEvent(
            UUID listingId,
            String equipmentName,
            String institutionName,
            UUID sharedById,
            UUID institutionId
    ) {}

    public record AccessRequestSubmittedEvent(
            UUID ownerInstitutionId,
            UUID requestId,
            String requesterName,
            String equipmentName,
            String requesterInstitutionName,
            UUID requesterId
    ) {}

    public record AccessRequestApprovedEvent(
            UUID requesterId,
            UUID requestId,
            String equipmentName
    ) {}

    public record AccessRequestRejectedEvent(
            UUID requesterId,
            UUID requestId,
            String equipmentName
    ) {}

    // ── Institution & Organisation Events ─────────────────────────────────────

    /**
     * Fired when System Admin approves (verifies) an institution.
     * Notifies: Institution Admins of that institution (they are now activated).
     */
    public record InstitutionApprovedEvent(
            UUID institutionId,
            String institutionName
    ) {}

    /**
     * Fired when System Admin suspends an institution.
     * Notifies: Institution Admins of that institution (they are now suspended).
     */
    public record InstitutionSuspendedEvent(
            UUID institutionId,
            String institutionName
    ) {}

    /**
     * Fired when a department is created.
     * Notifies: Institution Admins + Dept Heads of the same institution.
     * Excludes the creator (they get a self-confirm).
     */
    public record DepartmentCreatedEvent(
            UUID institutionId,
            UUID departmentId,
            String departmentName,
            UUID createdById,
            String createdByName
    ) {}

    /**
     * Fired when a department is updated.
     * Notifies: Institution Admins + Dept Heads of the same institution.
     */
    public record DepartmentUpdatedEvent(
            UUID institutionId,
            UUID departmentId,
            String departmentName,
            UUID updatedById,
            String updatedByName
    ) {}

    /**
     * Fired when a new equipment category is created.
     * Notifies: System Admins only (category is a global entity).
     * Actor gets a self-confirmation.
     */
    public record CategoryAddedEvent(
            UUID categoryId,
            String categoryName,
            UUID addedById,
            String addedByName
    ) {}

    // ── User & Profile Events ─────────────────────────────────────────────────

    /**
     * Fired when a user updates their own profile.
     * Constraint: ONLY the affected user receives this notification.
     * No admin or other user is ever notified of another person's profile change.
     */
    public record ProfileUpdatedEvent(
            UUID userId,
            String userName
    ) {}

    /**
     * Fired when an admin creates a new user account.
     * Notifies: The newly created user (welcome notification) +
     *           Institution Admins of the same institution.
     */
    public record UserCreatedEvent(
            UUID institutionId,
            UUID newUserId,
            String newUserName,
            String newUserEmail,
            UUID createdById,
            String createdByName
    ) {}

    /**
     * Fired when a user's active/suspended status is toggled.
     * Notifies: The affected user + Institution Admins of their institution.
     */
    public record UserStatusToggledEvent(
            UUID institutionId,
            UUID userId,
            String userName,
            boolean isNowActive,
            UUID adminId,
            String adminName
    ) {}

    /**
     * Fired when a role is assigned to a user.
     * Notifies: The affected user + Institution Admins of their institution.
     */
    public record UserRoleAssignedEvent(
            UUID institutionId,
            UUID userId,
            String userName,
            String roleName,
            UUID adminId,
            String adminName
    ) {}

    // ── Analytics & Reporting Events ──────────────────────────────────────────

    /**
     * Fired when an OEE/analytics report job completes.
     * Constraint: ONLY the user who requested the report is notified.
     */
    public record OeeReportReadyEvent(
            UUID requesterId,
            UUID reportId,
            String reportName
    ) {}
}
