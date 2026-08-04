package in.sbmtechservice.Lab_Resource_Utilization.notification.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.entity.Notification;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationChannel;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationReferenceType;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationStatus;
import in.sbmtechservice.Lab_Resource_Utilization.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Central Notification Dispatcher — the single point of truth for all in-app notifications.
 *
 * Design principles (real-world pattern):
 *  1. All business services call this class to fire notifications — never save Notification
 *     entities directly from business logic.
 *  2. Methods run in a SEPARATE transaction (REQUIRES_NEW) so that a notification failure
 *     never rolls back the originating business transaction.
 *  3. @Async ensures notification logic does not add latency to the primary request.
 *  4. Every method accepts explicit recipient lists for testability and clarity.
 *
 * To add EMAIL/SMS later: add a new NotificationChannel branch inside sendTo().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  Core send primitives
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send an IN_APP notification to a single user.
     * Runs in its own transaction so caller's transaction is never affected.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(User recipient,
                     NotificationReferenceType type,
                     UUID referenceId,
                     String content) {
        if (recipient == null) return;
        try {
            Notification notification = Notification.builder()
                    .user(recipient)
                    .referenceType(type)
                    .referenceId(referenceId)
                    .channel(NotificationChannel.IN_APP)
                    .content(content)
                    .status(NotificationStatus.SENT)
                    .isRead(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            log.debug("[NOTIFY] → {} ({}): {}", recipient.getEmail(), type, content);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to save notification for user {}: {}", recipient.getId(), e.getMessage());
        }
    }

    /**
     * Send to multiple recipients at once (bulk).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToAll(Collection<User> recipients,
                          NotificationReferenceType type,
                          UUID referenceId,
                          String content) {
        if (recipients == null || recipients.isEmpty()) return;
        List<Notification> batch = new ArrayList<>();
        for (User recipient : recipients) {
            if (recipient == null) continue;
            batch.add(Notification.builder()
                    .user(recipient)
                    .referenceType(type)
                    .referenceId(referenceId)
                    .channel(NotificationChannel.IN_APP)
                    .content(content)
                    .status(NotificationStatus.SENT)
                    .isRead(false)
                    .sentAt(LocalDateTime.now())
                    .build());
        }
        if (!batch.isEmpty()) {
            notificationRepository.saveAll(batch);
            log.debug("[NOTIFY] Batch {} notifications → type={}", batch.size(), type);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Role-based recipient helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Collect all system admins (global role, no institution scope).
     */
    public List<User> getSystemAdmins() {
        return userRepository.findAllByRoleName(RoleType.SYSTEM_ADMIN);
    }

    /**
     * Collect all institution admins for a given institution.
     */
    public List<User> getInstAdmins(UUID institutionId) {
        if (institutionId == null) return List.of();
        return userRepository.findByRoleAndInstitutionId(RoleType.INSTITUTION_ADMIN, institutionId);
    }

    /**
     * Collect all department heads for a given institution.
     */
    public List<User> getDeptHeads(UUID institutionId) {
        if (institutionId == null) return List.of();
        return userRepository.findByRoleAndInstitutionId(RoleType.DEPT_HEAD, institutionId);
    }

    /**
     * Collect all lab managers for a given institution.
     */
    public List<User> getLabManagers(UUID institutionId) {
        if (institutionId == null) return List.of();
        return userRepository.findByRoleAndInstitutionId(RoleType.LAB_MANAGER, institutionId);
    }

    /**
     * Collect all department heads for a specific department.
     */
    public List<User> getDeptHeadsForDept(UUID departmentId) {
        if (departmentId == null) return List.of();
        return userRepository.findByRoleAndDepartmentId(RoleType.DEPT_HEAD, departmentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  High-level domain-aware notification methods
    //  (called from business services — keeps business services clean)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * EQUIPMENT ADDED — Notify Dept Heads + Inst Admins of the institution + System Admins.
     */
    public void notifyEquipmentAdded(UUID institutionId, UUID equipmentId,
                                     String equipmentName, String addedByName) {
        String msg = String.format("New equipment '%s' has been added to your institution by %s.",
                equipmentName, addedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        recipients.addAll(getSystemAdmins());
        sendToAll(deduplicate(recipients), NotificationReferenceType.EQUIPMENT, equipmentId, msg);
    }

    /**
     * EQUIPMENT UPDATED — Notify Dept Heads + Inst Admins.
     */
    public void notifyEquipmentUpdated(UUID institutionId, UUID equipmentId,
                                       String equipmentName, String updatedByName) {
        String msg = String.format("Equipment '%s' details have been updated by %s.",
                equipmentName, updatedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        sendToAll(deduplicate(recipients), NotificationReferenceType.EQUIPMENT, equipmentId, msg);
    }

    /**
     * EQUIPMENT STATUS CHANGED — Notify Dept Heads + Inst Admins.
     * (e.g. AVAILABLE → UNDER_MAINTENANCE)
     */
    public void notifyEquipmentStatusChanged(UUID institutionId, UUID equipmentId,
                                             String equipmentName, String oldStatus,
                                             String newStatus, String changedByName) {
        String msg = String.format("Equipment '%s' status changed from %s to %s by %s.",
                equipmentName, oldStatus, newStatus, changedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        sendToAll(deduplicate(recipients), NotificationReferenceType.EQUIPMENT, equipmentId, msg);
    }

    /**
     * BOOKING CREATED (PENDING) — Notify Lab Managers + Dept Heads + Inst Admin of
     * the equipment's institution so they can approve.
     */
    public void notifyBookingPendingApproval(UUID equipmentInstitutionId, UUID bookingId,
                                             String bookerName, String equipmentName,
                                             String startTime) {
        String msg = String.format(
                "New booking request from %s for '%s' starting %s is awaiting approval.",
                bookerName, equipmentName, startTime);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getLabManagers(equipmentInstitutionId));
        recipients.addAll(getDeptHeads(equipmentInstitutionId));
        recipients.addAll(getInstAdmins(equipmentInstitutionId));
        sendToAll(deduplicate(recipients), NotificationReferenceType.BOOKING_APPROVAL_REQUEST, bookingId, msg);
    }

    /**
     * BOOKING CREATED (PENDING_PAYMENT) — Notify the booker to pay their invoice.
     */
    public void notifyBookingInvoiceGenerated(User booker, UUID bookingId,
                                              String equipmentName, String amount) {
        String msg = String.format(
                "Your booking for '%s' has been created. Invoice of ₹%s has been generated. Please complete payment.",
                equipmentName, amount);
        send(booker, NotificationReferenceType.INVOICE, bookingId, msg);
    }

    /**
     * BOOKING CONFIRMED — Notify the booker their booking was approved.
     */
    public void notifyBookingConfirmed(User booker, UUID bookingId,
                                       String equipmentName, String startTime) {
        String msg = String.format(
                "Your booking for '%s' on %s has been CONFIRMED. You're all set!",
                equipmentName, startTime);
        send(booker, NotificationReferenceType.BOOKING, bookingId, msg);
    }

    /**
     * BOOKING CANCELLED — Notify the booker + Dept Heads/Lab Managers of the equipment's institution.
     */
    public void notifyBookingCancelled(User booker, UUID equipmentInstitutionId,
                                       UUID bookingId, String equipmentName,
                                       String cancelledByName) {
        // Notify the booker
        send(booker, NotificationReferenceType.BOOKING, bookingId,
                String.format("Your booking for '%s' has been CANCELLED by %s.", equipmentName, cancelledByName));

        // Notify institution staff (if cancelled by someone other than the booker)
        List<User> staff = new ArrayList<>();
        staff.addAll(getLabManagers(equipmentInstitutionId));
        staff.addAll(getDeptHeads(equipmentInstitutionId));
        staff.remove(booker); // don't double-notify if booker is staff
        String staffMsg = String.format("Booking for '%s' by %s has been cancelled.", equipmentName, booker.getFirstName() + " " + booker.getLastName());
        sendToAll(deduplicate(staff), NotificationReferenceType.BOOKING, bookingId, staffMsg);
    }

    /**
     * BOOKING COMPLETED — Notify the booker their session is complete.
     */
    public void notifyBookingCompleted(User booker, UUID bookingId, String equipmentName) {
        String msg = String.format(
                "Your booking session for '%s' has been marked as COMPLETED. Thank you!",
                equipmentName);
        send(booker, NotificationReferenceType.BOOKING, bookingId, msg);
    }

    /**
     * MAINTENANCE SCHEDULED — Notify technician + Dept Heads + Inst Admins.
     */
    public void notifyMaintenanceScheduled(User technician, UUID institutionId,
                                           UUID taskId, String equipmentName,
                                           String scheduledDate) {
        // Notify the assigned technician
        if (technician != null) {
            send(technician, NotificationReferenceType.MAINTENANCE, taskId,
                    String.format("You have been assigned a maintenance task for '%s' on %s.",
                            equipmentName, scheduledDate));
        }
        // Notify Dept Heads + Inst Admins
        String mgmtMsg = String.format(
                "Maintenance has been scheduled for '%s' on %s. Equipment is now locked.",
                equipmentName, scheduledDate);
        List<User> mgmt = new ArrayList<>();
        mgmt.addAll(getDeptHeads(institutionId));
        mgmt.addAll(getInstAdmins(institutionId));
        sendToAll(deduplicate(mgmt), NotificationReferenceType.MAINTENANCE, taskId, mgmtMsg);
    }

    /**
     * MAINTENANCE COMPLETED — Notify Dept Heads + Inst Admins that equipment is back online.
     */
    public void notifyMaintenanceCompleted(UUID institutionId, UUID taskId, String equipmentName) {
        String msg = String.format(
                "Maintenance for '%s' has been COMPLETED. Equipment is now back online and available.",
                equipmentName);
        List<User> mgmt = new ArrayList<>();
        mgmt.addAll(getDeptHeads(institutionId));
        mgmt.addAll(getInstAdmins(institutionId));
        sendToAll(deduplicate(mgmt), NotificationReferenceType.MAINTENANCE, taskId, msg);
    }

    /**
     * RESOURCE SHARE LISTED — Notify System Admins that an institute listed equipment for sharing.
     */
    public void notifyResourceShareListed(UUID listingId, String equipmentName, String institutionName) {
        String msg = String.format(
                "'%s' from %s has been listed for inter-institution sharing.",
                equipmentName, institutionName);
        sendToAll(getSystemAdmins(), NotificationReferenceType.SHARING_REQUEST, listingId, msg);
    }

    /**
     * ACCESS REQUEST SUBMITTED — Notify Inst Admins of the equipment-owning institution.
     */
    public void notifyAccessRequestSubmitted(UUID ownerInstitutionId, UUID requestId,
                                             String requesterName, String equipmentName,
                                             String requesterInstitutionName) {
        String msg = String.format(
                "%s from %s has requested access to your shared equipment '%s'. Please review.",
                requesterName, requesterInstitutionName, equipmentName);
        sendToAll(getInstAdmins(ownerInstitutionId), NotificationReferenceType.ACCESS_REQUEST, requestId, msg);
    }

    /**
     * ACCESS REQUEST APPROVED — Notify the requester.
     */
    public void notifyAccessRequestApproved(User requester, UUID requestId, String equipmentName) {
        send(requester, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("Your access request for '%s' has been APPROVED. You can now book this equipment.",
                        equipmentName));
    }

    /**
     * ACCESS REQUEST REJECTED — Notify the requester.
     */
    public void notifyAccessRequestRejected(User requester, UUID requestId, String equipmentName) {
        send(requester, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("Your access request for '%s' has been REJECTED. Contact the institution admin for details.",
                        equipmentName));
    }

    /**
     * WAITLIST FULFILLED — Notify user they got a slot from the waitlist.
     */
    public void notifyWaitlistFulfilled(User user, UUID waitlistId, String equipmentName) {
        send(user, NotificationReferenceType.WAITLIST, waitlistId,
                String.format("A slot for '%s' became available and your waitlist request has been fulfilled! A new booking has been created for you.",
                        equipmentName));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Remove duplicate users from a recipient list (users may have multiple roles).
     */
    private List<User> deduplicate(List<User> users) {
        return users.stream()
                .filter(u -> u != null && u.getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        User::getId,
                        u -> u,
                        (existing, dup) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(java.util.stream.Collectors.toList());
    }
}
