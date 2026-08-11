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

/**
 * Central listener that bridges domain events → NotificationDispatcher.
 *
 * Rules:
 *  - @TransactionalEventListener(AFTER_COMMIT) for all business-service-emitted events
 *    (ensures the entity exists in DB before we reference it in a notification).
 *  - @EventListener for cron-fired events (no transaction context).
 *  - @Async so notification processing never blocks the business thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Async
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;
    private final EmailNotificationService emailService;
    private final UserRepository userRepository;

    // ── Equipment ─────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentAdded(NotificationEvents.EquipmentAddedEvent event) {
        log.info("[EVENT] EquipmentAdded — {}", event.equipmentName());
        dispatcher.notifyEquipmentAdded(
                event.institutionId(), event.equipmentId(), event.equipmentName(),
                event.addedById(), event.addedByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentUpdated(NotificationEvents.EquipmentUpdatedEvent event) {
        log.info("[EVENT] EquipmentUpdated — {}", event.equipmentName());
        dispatcher.notifyEquipmentUpdated(
                event.institutionId(), event.equipmentId(), event.equipmentName(), event.updatedByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEquipmentStatusChanged(NotificationEvents.EquipmentStatusChangedEvent event) {
        log.info("[EVENT] EquipmentStatusChanged — {} → {}", event.oldStatus(), event.newStatus());
        dispatcher.notifyEquipmentStatusChanged(
                event.institutionId(), event.equipmentId(), event.equipmentName(),
                event.oldStatus(), event.newStatus(), event.changedByName()
        );
    }

    // ── Booking ───────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingPendingApproval(NotificationEvents.BookingPendingApprovalEvent event) {
        log.info("[EVENT] BookingPendingApproval — {}", event.bookingId());
        dispatcher.notifyBookingPendingApproval(
                event.equipmentInstitutionId(), event.bookingId(), event.bookerName(),
                event.equipmentName(), event.startTime()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingInvoiceGenerated(NotificationEvents.BookingInvoiceGeneratedEvent event) {
        log.info("[EVENT] BookingInvoiceGenerated — {}", event.bookingId());
        dispatcher.notifyBookingInvoiceGenerated(
                event.bookerId(), event.bookingId(), event.equipmentName(), event.amount()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(NotificationEvents.BookingConfirmedEvent event) {
        log.info("[EVENT] BookingConfirmed — {}", event.bookingId());
        dispatcher.notifyBookingConfirmed(
                event.bookerId(), event.bookingId(), event.equipmentName(), event.startTime()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(NotificationEvents.BookingCancelledEvent event) {
        log.info("[EVENT] BookingCancelled — {}", event.bookingId());
        dispatcher.notifyBookingCancelled(
                event.bookerId(), event.equipmentInstitutionId(), event.bookingId(),
                event.equipmentName(), event.cancelledByName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCompleted(NotificationEvents.BookingCompletedEvent event) {
        log.info("[EVENT] BookingCompleted — {}", event.bookingId());
        dispatcher.notifyBookingCompleted(
                event.bookerId(), event.bookingId(), event.equipmentName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWaitlistFulfilled(NotificationEvents.WaitlistFulfilledEvent event) {
        log.info("[EVENT] WaitlistFulfilled — user {}", event.userId());
        dispatcher.notifyWaitlistFulfilled(
                event.userId(), event.waitlistId(), event.equipmentName()
        );
    }

    // ── Maintenance & Work Orders ─────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceScheduled(NotificationEvents.MaintenanceScheduledEvent event) {
        log.info("[EVENT] MaintenanceScheduled — {}", event.equipmentName());
        dispatcher.notifyMaintenanceScheduled(
                event.technicianId(), event.institutionId(), event.taskId(),
                event.equipmentName(), event.scheduledDate()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMaintenanceCompleted(NotificationEvents.MaintenanceCompletedEvent event) {
        log.info("[EVENT] MaintenanceCompleted — {}", event.equipmentName());
        dispatcher.notifyMaintenanceCompleted(
                event.institutionId(), event.taskId(), event.equipmentName()
        );
    }

    /**
     * WORK ORDER STATUS CHANGED — fires on every state transition.
     * Recipient: assigned technician (if any) + Dept Heads + Inst Admins (institution-scoped).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkOrderStatusChanged(NotificationEvents.WorkOrderStatusChangedEvent event) {
        log.info("[EVENT] WorkOrderStatusChanged — {} → {}", event.equipmentName(), event.newStatus());
        dispatcher.notifyWorkOrderStatusChanged(
                event.institutionId(), event.technicianId(), event.workOrderId(),
                event.equipmentName(), event.newStatus(), event.changedById(), event.changedByName()
        );
    }

    /**
     * WORK ORDER URGENT — fired for HIGH/CRITICAL priority work orders.
     * Emails System Admins + Institution Admins.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkOrderUrgent(NotificationEvents.WorkOrderUrgentEvent event) {
        log.info("[EVENT] WorkOrderUrgent — {} [{}]", event.equipmentName(), event.priority());

        dispatcher.sendToAll(dispatcher.getSystemAdmins(),
                NotificationReferenceType.MAINTENANCE, event.workOrderId(),
                String.format("🚨 Urgent Work Order [%s]: '%s' is now %s",
                        event.priority(), event.equipmentName(), event.currentStatus()));

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

            recipients.forEach(u -> {
                if (u.getEmail() != null) {
                    emailService.sendAssetDowntime(u.getEmail(), event.equipmentName(), event.estimatedDowntimeHours());
                }
            });
        }
    }

    // ── Calibration ───────────────────────────────────────────────────────────

    /**
     * CALIBRATION LOGGED — fired when a new calibration record is saved.
     * Audience: Lab Managers + Dept Heads + Inst Admins of equipment's institution.
     * Actor gets a self-confirmation.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCalibrationLogged(NotificationEvents.CalibrationLoggedEvent event) {
        log.info("[EVENT] CalibrationLogged — {} (next due: {})", event.equipmentName(), event.expiryDate());
        dispatcher.notifyCalibrationLogged(
                event.institutionId(), event.calibrationId(), event.equipmentName(),
                event.expiryDate(), event.loggedById(), event.loggedByName()
        );
    }

    /**
     * CALIBRATION REMINDER — fired by the daily cron job.
     * Sends in-app notification AND email to all Lab Managers of the institution.
     */
    @EventListener  // Not @TransactionalEventListener: fired from cron, no transaction context
    public void onCalibrationReminder(NotificationEvents.CalibrationReminderEvent event) {
        log.info("[EVENT] CalibrationReminder — {} expires in {}d", event.equipmentName(), event.daysUntilExpiry());

        if (event.institutionId() != null) {
            List<User> managers = new java.util.ArrayList<>(dispatcher.getLabManagers(event.institutionId()));
            managers.addAll(dispatcher.getDeptHeads(event.institutionId()));
            String msg = String.format("⚠️ Calibration for '%s' expires in %d day(s) on %s. Action required!",
                    event.equipmentName(), event.daysUntilExpiry(), event.expiryDate());
            dispatcher.sendToAll(managers, NotificationReferenceType.CALIBRATION, event.calibrationRecordId(), msg);

            managers.forEach(u -> {
                if (u.getEmail() != null) {
                    emailService.sendCalibrationReminder(
                            u.getEmail(), event.equipmentName(), event.expiryDate(), event.daysUntilExpiry());
                }
            });
        }
    }

    // ── Billing ───────────────────────────────────────────────────────────────

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

        String msg = String.format("✅ Invoice ₹%s has been approved and issued to your department.",
                event.invoiceAmount());
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

    // ── Resource Sharing ──────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResourceShareListed(NotificationEvents.ResourceShareListedEvent event) {
        log.info("[EVENT] ResourceShareListed — {}", event.equipmentName());
        dispatcher.notifyResourceShareListed(
                event.listingId(), event.equipmentName(), event.institutionName(),
                event.sharedById(), event.institutionId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestSubmitted(NotificationEvents.AccessRequestSubmittedEvent event) {
        log.info("[EVENT] AccessRequestSubmitted — {}", event.requestId());
        dispatcher.notifyAccessRequestSubmitted(
                event.ownerInstitutionId(), event.requestId(), event.requesterName(),
                event.equipmentName(), event.requesterInstitutionName(), event.requesterId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestApproved(NotificationEvents.AccessRequestApprovedEvent event) {
        log.info("[EVENT] AccessRequestApproved — {}", event.requestId());
        dispatcher.notifyAccessRequestApproved(
                event.requesterId(), event.requestId(), event.equipmentName()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccessRequestRejected(NotificationEvents.AccessRequestRejectedEvent event) {
        log.info("[EVENT] AccessRequestRejected — {}", event.requestId());
        dispatcher.notifyAccessRequestRejected(
                event.requesterId(), event.requestId(), event.equipmentName()
        );
    }

    // ── Institution & Organisation ────────────────────────────────────────────

    /**
     * INSTITUTION APPROVED — Notify Institution Admins of that institution.
     * Audience strictly scoped: other institutions never see this event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstitutionApproved(NotificationEvents.InstitutionApprovedEvent event) {
        log.info("[EVENT] InstitutionApproved — {}", event.institutionName());
        dispatcher.notifyInstitutionApproved(event.institutionId(), event.institutionName());
    }

    /**
     * INSTITUTION SUSPENDED — Notify Institution Admins of that institution.
     * Audience strictly scoped: other institutions never see this event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstitutionSuspended(NotificationEvents.InstitutionSuspendedEvent event) {
        log.info("[EVENT] InstitutionSuspended — {}", event.institutionName());
        dispatcher.notifyInstitutionSuspended(event.institutionId(), event.institutionName());
    }

    /**
     * DEPARTMENT CREATED — Notify Inst Admins + Dept Heads of the same institution.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepartmentCreated(NotificationEvents.DepartmentCreatedEvent event) {
        log.info("[EVENT] DepartmentCreated — {} in institution {}", event.departmentName(), event.institutionId());
        dispatcher.notifyDepartmentCreated(
                event.institutionId(), event.departmentId(), event.departmentName(),
                event.createdById(), event.createdByName()
        );
    }

    /**
     * DEPARTMENT UPDATED — Notify Inst Admins + Dept Heads of the same institution.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepartmentUpdated(NotificationEvents.DepartmentUpdatedEvent event) {
        log.info("[EVENT] DepartmentUpdated — {}", event.departmentName());
        dispatcher.notifyDepartmentUpdated(
                event.institutionId(), event.departmentId(), event.departmentName(),
                event.updatedById(), event.updatedByName()
        );
    }

    /**
     * CATEGORY ADDED — Notify System Admins only (global entity).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryAdded(NotificationEvents.CategoryAddedEvent event) {
        log.info("[EVENT] CategoryAdded — {}", event.categoryName());
        dispatcher.notifyCategoryAdded(
                event.categoryId(), event.categoryName(), event.addedById(), event.addedByName()
        );
    }

    // ── User & Profile ────────────────────────────────────────────────────────

    /**
     * PROFILE UPDATED — SELF ONLY.
     * This handler deliberately only calls notifyProfileUpdated which sends to the user ONLY.
     * No broadcast, no admin notification.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(NotificationEvents.ProfileUpdatedEvent event) {
        log.info("[EVENT] ProfileUpdated — user {}", event.userId());
        dispatcher.notifyProfileUpdated(event.userId(), event.userName());
    }

    /**
     * USER CREATED — Welcome the new user + notify Inst Admins.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(NotificationEvents.UserCreatedEvent event) {
        log.info("[EVENT] UserCreated — {} ({})", event.newUserName(), event.newUserEmail());
        dispatcher.notifyUserCreated(
                event.institutionId(), event.newUserId(), event.newUserName(),
                event.newUserEmail(), event.createdById(), event.createdByName()
        );
    }

    /**
     * USER STATUS TOGGLED — Notify the affected user + admins.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserStatusToggled(NotificationEvents.UserStatusToggledEvent event) {
        log.info("[EVENT] UserStatusToggled — {} isActive={}", event.userName(), event.isNowActive());
        dispatcher.notifyUserStatusToggled(
                event.institutionId(), event.userId(), event.userName(),
                event.isNowActive(), event.adminId(), event.adminName()
        );
    }

    /**
     * USER ROLE ASSIGNED — Notify the affected user + admins.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRoleAssigned(NotificationEvents.UserRoleAssignedEvent event) {
        log.info("[EVENT] UserRoleAssigned — role={} to user {}", event.roleName(), event.userName());
        dispatcher.notifyUserRoleAssigned(
                event.institutionId(), event.userId(), event.userName(),
                event.roleName(), event.adminId(), event.adminName()
        );
    }

    // ── Reporting ─────────────────────────────────────────────────────────────

    /**
     * OEE REPORT READY — REQUESTER ONLY.
     * No @TransactionalEventListener since this fires from async job completion.
     */
    @EventListener
    public void onOeeReportReady(NotificationEvents.OeeReportReadyEvent event) {
        log.info("[EVENT] OeeReportReady — reportId={} for user {}", event.reportId(), event.requesterId());
        dispatcher.notifyOeeReportReady(event.requesterId(), event.reportId(), event.reportName());
    }
}
