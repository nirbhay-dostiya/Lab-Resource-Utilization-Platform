package in.sbmtechservice.Lab_Resource_Utilization.notification.event;

import java.util.UUID;

/**
 * Domain events for the notification system.
 * Emitted by business services to decouple transaction commits from async notification delivery.
 *
 * New events added for enterprise feature expansion:
 *  - AssetDowntimeEvent       (Module 6 — asset.downtime trigger)
 *  - WorkOrderUrgentEvent     (Module 6 — workorder.urgent trigger)
 *  - BillingFailedEvent       (Module 6 — billing.failed trigger)
 *  - CalibrationReminderEvent (Module 2 + Module 6 — calibration.reminder trigger)
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
}
