package in.sbmtechservice.Lab_Resource_Utilization.notification.event;

import java.util.UUID;

/**
 * Domain events for the notification system.
 * Emitted by business services to decouple transaction commits from async notification delivery.
 */
public class NotificationEvents {

    public record EquipmentAddedEvent(
            UUID institutionId,
            UUID equipmentId,
            String equipmentName,
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

    public record ResourceShareListedEvent(
            UUID listingId,
            String equipmentName,
            String institutionName
    ) {}

    public record AccessRequestSubmittedEvent(
            UUID ownerInstitutionId,
            UUID requestId,
            String requesterName,
            String equipmentName,
            String requesterInstitutionName
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
