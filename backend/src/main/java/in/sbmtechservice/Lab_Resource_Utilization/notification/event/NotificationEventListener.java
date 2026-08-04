package in.sbmtechservice.Lab_Resource_Utilization.notification.event;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationReferenceType;
import in.sbmtechservice.Lab_Resource_Utilization.notification.service.EmailNotificationService;
import in.sbmtechservice.Lab_Resource_Utilization.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Async
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;
    private final EmailNotificationService emailService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentAdded(NotificationEvents.EquipmentAddedEvent event) {
        dispatcher.notifyEquipmentAdded(
                event.institutionId(), event.equipmentId(), event.equipmentName(), event.addedById(), event.addedByName()
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
                event.listingId(), event.equipmentName(), event.institutionName(),
                event.sharedById(), event.institutionId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestSubmitted(NotificationEvents.AccessRequestSubmittedEvent event) {
        dispatcher.notifyAccessRequestSubmitted(
                event.ownerInstitutionId(), event.requestId(), event.requesterName(),
                event.equipmentName(), event.requesterInstitutionName(), event.requesterId()
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

    // ── Module 6: New Enterprise Event Handlers ────────────────────────────────

    /**
     * CALIBRATION REMINDER — fired by the daily cron job.
     * Sends an in-app notification AND an email to all Lab Managers of the institution.
     */
    @EventListener  // Not @TransactionalEventListener: fired from cron, no transaction context
    public void onCalibrationReminder(NotificationEvents.CalibrationReminderEvent event) {
        log.info("[EVENT] CalibrationReminder — {} expires in {}d", event.equipmentName(), event.daysUntilExpiry());

        // In-app: notify all lab managers of the institution
        if (event.institutionId() != null) {
            List<User> managers = dispatcher.getLabManagers(event.institutionId());
            managers.addAll(dispatcher.getDeptHeads(event.institutionId()));
            String msg = String.format("⚠️ Calibration for '%s' expires in %d day(s) on %s. Action required!",
                    event.equipmentName(), event.daysUntilExpiry(), event.expiryDate());
            dispatcher.sendToAll(managers, NotificationReferenceType.MAINTENANCE, event.calibrationRecordId(), msg);

            // Email all managers
            managers.forEach(u -> {
                if (u.getEmail() != null) {
                    emailService.sendCalibrationReminder(
                            u.getEmail(), event.equipmentName(), event.expiryDate(), event.daysUntilExpiry());
                }
            });
        }
    }

    /**
     * WORK ORDER URGENT — fired when a HIGH/CRITICAL work order transitions state.
     * Emails System Admins + Institution Admins.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkOrderUrgent(NotificationEvents.WorkOrderUrgentEvent event) {
        log.info("[EVENT] WorkOrderUrgent — {} [{}]", event.equipmentName(), event.priority());

        // In-app notify system admins
        dispatcher.sendToAll(dispatcher.getSystemAdmins(),
                NotificationReferenceType.MAINTENANCE, event.workOrderId(),
                String.format("🚨 Urgent Work Order [%s]: '%s' is %s",
                        event.priority(), event.equipmentName(), event.currentStatus()));

        // Email institution admins
        if (event.institutionId() != null) {
            dispatcher.getInstAdmins(event.institutionId()).forEach(u -> {
                if (u.getEmail() != null) {
                    emailService.sendWorkOrderUrgent(u.getEmail(), event.equipmentName(),
                            event.priority(), event.currentStatus());
                }
            });
        }
    }

    /**
     * ASSET DOWNTIME — fired when equipment enters UNDER_MAINTENANCE state.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssetDowntime(NotificationEvents.AssetDowntimeEvent event) {
        log.info("[EVENT] AssetDowntime — {}", event.equipmentName());

        String msg = String.format("🔧 '%s' is now UNDER MAINTENANCE. Estimated downtime: %s hours.",
                event.equipmentName(), event.estimatedDowntimeHours());

        if (event.institutionId() != null) {
            List<User> recipients = new java.util.ArrayList<>(dispatcher.getDeptHeads(event.institutionId()));
            recipients.addAll(dispatcher.getInstAdmins(event.institutionId()));
            dispatcher.sendToAll(recipients, NotificationReferenceType.MAINTENANCE, event.equipmentId(), msg);

            // Email notification
            recipients.forEach(u -> {
                if (u.getEmail() != null) {
                    emailService.sendAssetDowntime(u.getEmail(), event.equipmentName(), event.estimatedDowntimeHours());
                }
            });
        }
    }

    /**
     * INVOICE APPROVAL REQUESTED — fired when a DRAFT invoice is submitted.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceApprovalRequested(NotificationEvents.InvoiceApprovalRequestedEvent event) {
        log.info("[EVENT] InvoiceApprovalRequested — ₹{}", event.invoiceAmount());

        String msg = String.format("📄 Invoice ₹%s is pending your approval.", event.invoiceAmount());
        if (event.institutionId() != null) {
            List<User> approvers = new java.util.ArrayList<>(dispatcher.getInstAdmins(event.institutionId()));
            approvers.addAll(dispatcher.getDeptHeads(event.institutionId()));
            dispatcher.sendToAll(approvers, NotificationReferenceType.INVOICE, event.invoiceId(), msg);

            approvers.forEach(u -> {
                if (u.getEmail() != null) {
                    emailService.sendInvoiceApprovalRequest(u.getEmail(),
                            event.invoiceId().toString(), event.invoiceAmount());
                }
            });
        }
    }

    /**
     * INVOICE APPROVED — fired when an invoice is approved and moves to ISSUED.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceApproved(NotificationEvents.InvoiceApprovedEvent event) {
        log.info("[EVENT] InvoiceApproved — {}", event.invoiceId());

        // Notify the billed department's members via in-app
        String msg = String.format("✅ Invoice ₹%s has been approved and issued to your department.",
                event.invoiceAmount());
        // Send to dept users (limited to dept heads here)
        if (event.issuedToDepartmentId() != null) {
            dispatcher.getDeptHeadsForDept(event.issuedToDepartmentId())
                    .forEach(u -> {
                        dispatcher.send(u.getId(), NotificationReferenceType.INVOICE, event.invoiceId(), msg);
                        if (u.getEmail() != null) {
                            emailService.sendInvoiceApproved(u.getEmail(),
                                    event.invoiceId().toString(), event.invoiceAmount());
                        }
                    });
        }
    }
}

