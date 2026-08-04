package in.sbmtechservice.Lab_Resource_Utilization.notification.event;

import in.sbmtechservice.Lab_Resource_Utilization.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentAdded(NotificationEvents.EquipmentAddedEvent event) {
        dispatcher.notifyEquipmentAdded(
                event.institutionId(), event.equipmentId(), event.equipmentName(), event.addedByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentUpdated(NotificationEvents.EquipmentUpdatedEvent event) {
        dispatcher.notifyEquipmentUpdated(
                event.institutionId(), event.equipmentId(), event.equipmentName(), event.updatedByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentStatusChanged(NotificationEvents.EquipmentStatusChangedEvent event) {
        dispatcher.notifyEquipmentStatusChanged(
                event.institutionId(), event.equipmentId(), event.equipmentName(),
                event.oldStatus(), event.newStatus(), event.changedByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingPendingApproval(NotificationEvents.BookingPendingApprovalEvent event) {
        dispatcher.notifyBookingPendingApproval(
                event.equipmentInstitutionId(), event.bookingId(), event.bookerName(),
                event.equipmentName(), event.startTime()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingInvoiceGenerated(NotificationEvents.BookingInvoiceGeneratedEvent event) {
        dispatcher.notifyBookingInvoiceGenerated(
                event.bookerId(), event.bookingId(), event.equipmentName(), event.amount()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(NotificationEvents.BookingConfirmedEvent event) {
        dispatcher.notifyBookingConfirmed(
                event.bookerId(), event.bookingId(), event.equipmentName(), event.startTime()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(NotificationEvents.BookingCancelledEvent event) {
        dispatcher.notifyBookingCancelled(
                event.bookerId(), event.equipmentInstitutionId(), event.bookingId(),
                event.equipmentName(), event.cancelledByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCompleted(NotificationEvents.BookingCompletedEvent event) {
        dispatcher.notifyBookingCompleted(
                event.bookerId(), event.bookingId(), event.equipmentName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWaitlistFulfilled(NotificationEvents.WaitlistFulfilledEvent event) {
        dispatcher.notifyWaitlistFulfilled(
                event.userId(), event.waitlistId(), event.equipmentName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceScheduled(NotificationEvents.MaintenanceScheduledEvent event) {
        dispatcher.notifyMaintenanceScheduled(
                event.technicianId(), event.institutionId(), event.taskId(),
                event.equipmentName(), event.scheduledDate()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceCompleted(NotificationEvents.MaintenanceCompletedEvent event) {
        dispatcher.notifyMaintenanceCompleted(
                event.institutionId(), event.taskId(), event.equipmentName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResourceShareListed(NotificationEvents.ResourceShareListedEvent event) {
        dispatcher.notifyResourceShareListed(
                event.listingId(), event.equipmentName(), event.institutionName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestSubmitted(NotificationEvents.AccessRequestSubmittedEvent event) {
        dispatcher.notifyAccessRequestSubmitted(
                event.ownerInstitutionId(), event.requestId(), event.requesterName(),
                event.equipmentName(), event.requesterInstitutionName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestApproved(NotificationEvents.AccessRequestApprovedEvent event) {
        dispatcher.notifyAccessRequestApproved(
                event.requesterId(), event.requestId(), event.equipmentName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestRejected(NotificationEvents.AccessRequestRejectedEvent event) {
        dispatcher.notifyAccessRequestRejected(
                event.requesterId(), event.requestId(), event.equipmentName()
        );
    }
}
