package in.sbmtechservice.Lab_Resource_Utilization.notification.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.dto.NotificationResponse;
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
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseService sseService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Core send primitives
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send an IN_APP notification to a single user.
     * Runs in its own transaction so caller's transaction is never affected.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(UUID recipientId,
                     NotificationReferenceType type,
                     UUID referenceId,
                     String content) {
        if (recipientId == null) return;
        try {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null) return;
            
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
            sseService.sendNotification(recipientId, mapToResponse(notification));
            log.debug("[NOTIFY] → {} ({}): {}", recipient.getEmail(), type, content);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to save notification for user {}: {}", recipientId, e.getMessage());
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
            for (Notification n : batch) {
                sseService.sendNotification(n.getUser().getId(), mapToResponse(n));
            }
            log.debug("[NOTIFY] Bulk sent {} notifications of type {}", batch.size(), type);
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
                                     String equipmentName, UUID addedById, String addedByName) {
        
        // Explicitly notify the user who added it
        send(addedById, NotificationReferenceType.EQUIPMENT, equipmentId,
                String.format("You have successfully added equipment '%s'.", equipmentName));

        String msg = String.format("New equipment '%s' has been added to your institution by %s.",
                equipmentName, addedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        recipients.addAll(getSystemAdmins());
        
        // Prevent sending the third-person message to the user who added it
        recipients.removeIf(u -> u.getId().equals(addedById));
        
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
    public void notifyBookingInvoiceGenerated(UUID bookerId, UUID bookingId,
                                              String equipmentName, String amount) {
        String msg = String.format(
                "Your booking for '%s' has been created. Invoice of ₹%s has been generated. Please complete payment.",
                equipmentName, amount);
        send(bookerId, NotificationReferenceType.INVOICE, bookingId, msg);
    }

    /**
     * BOOKING CONFIRMED — Notify the booker their booking was approved.
     */
    public void notifyBookingConfirmed(UUID bookerId, UUID bookingId,
                                       String equipmentName, String startTime) {
        String msg = String.format(
                "Your booking for '%s' on %s has been CONFIRMED. You're all set!",
                equipmentName, startTime);
        send(bookerId, NotificationReferenceType.BOOKING, bookingId, msg);
    }

    /**
     * BOOKING CANCELLED — Notify the booker + Dept Heads/Lab Managers of the equipment's institution.
     */
    public void notifyBookingCancelled(UUID bookerId, UUID equipmentInstitutionId,
                                       UUID bookingId, String equipmentName,
                                       String cancelledByName) {
        // Notify the booker
        send(bookerId, NotificationReferenceType.BOOKING, bookingId,
                String.format("Your booking for '%s' has been CANCELLED by %s.", equipmentName, cancelledByName));

        // Notify institution staff (if cancelled by someone other than the booker)
        List<User> staff = new ArrayList<>();
        staff.addAll(getLabManagers(equipmentInstitutionId));
        staff.addAll(getDeptHeads(equipmentInstitutionId));
        staff.removeIf(u -> u.getId().equals(bookerId)); // don't double-notify if booker is staff
        String staffMsg = String.format("Booking for '%s' has been cancelled.", equipmentName);
        sendToAll(deduplicate(staff), NotificationReferenceType.BOOKING, bookingId, staffMsg);
    }

    /**
     * BOOKING COMPLETED — Notify the booker their session is complete.
     */
    public void notifyBookingCompleted(UUID bookerId, UUID bookingId, String equipmentName) {
        String msg = String.format(
                "Your booking session for '%s' has been marked as COMPLETED. Thank you!",
                equipmentName);
        send(bookerId, NotificationReferenceType.BOOKING, bookingId, msg);
    }

    /**
     * MAINTENANCE SCHEDULED — Notify technician + Dept Heads + Inst Admins.
     */
    public void notifyMaintenanceScheduled(UUID technicianId, UUID institutionId,
                                           UUID taskId, String equipmentName,
                                           String scheduledDate) {
        // Notify the assigned technician
        if (technicianId != null) {
            send(technicianId, NotificationReferenceType.MAINTENANCE, taskId,
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
     * RESOURCE SHARE LISTED — Notify the person who listed it + Dept Heads + Inst Admins + System Admins.
     */
    public void notifyResourceShareListed(UUID listingId, String equipmentName, String institutionName, UUID sharedById, UUID institutionId) {
        // Notify the user who shared it
        send(sharedById, NotificationReferenceType.SHARING_REQUEST, listingId,
                String.format("You have successfully listed '%s' for inter-institution sharing.", equipmentName));

        // Notify Dept Heads and Inst Admins of the owning institution
        String staffMsg = String.format("Equipment '%s' has been listed for inter-institution sharing.", equipmentName);
        List<User> staff = new ArrayList<>();
        staff.addAll(getDeptHeads(institutionId));
        staff.addAll(getInstAdmins(institutionId));
        staff.removeIf(u -> u.getId().equals(sharedById)); // Don't double notify the person who shared it
        sendToAll(deduplicate(staff), NotificationReferenceType.SHARING_REQUEST, listingId, staffMsg);

        // Notify System Admins
        String sysAdminMsg = String.format("'%s' from %s has been listed for inter-institution sharing.", equipmentName, institutionName);
        sendToAll(getSystemAdmins(), NotificationReferenceType.SHARING_REQUEST, listingId, sysAdminMsg);
    }

    /**
     * ACCESS REQUEST SUBMITTED — Notify Inst Admins, Dept Heads, and Lab Managers of the equipment-owning institution, and the requester.
     */
    public void notifyAccessRequestSubmitted(UUID ownerInstitutionId, UUID requestId,
                                             String requesterName, String equipmentName,
                                             String requesterInstitutionName, UUID requesterId) {
        // Notify the requester
        send(requesterId, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("Your access request for '%s' has been successfully submitted.", equipmentName));

        // Notify the owning institution's staff
        String msg = String.format(
                "%s from %s has requested access to your shared equipment '%s'. Please review.",
                requesterName, requesterInstitutionName, equipmentName);
        
        List<User> staff = new ArrayList<>();
        staff.addAll(getLabManagers(ownerInstitutionId));
        staff.addAll(getDeptHeads(ownerInstitutionId));
        staff.addAll(getInstAdmins(ownerInstitutionId));
        staff.removeIf(u -> u.getId().equals(requesterId)); // Don't notify the requester if they are also staff
        sendToAll(deduplicate(staff), NotificationReferenceType.ACCESS_REQUEST, requestId, msg);
    }

    /**
     * ACCESS REQUEST APPROVED — Notify the requester.
     */
    public void notifyAccessRequestApproved(UUID requesterId, UUID requestId, String equipmentName) {
        send(requesterId, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("Your access request for '%s' has been APPROVED. You can now book this equipment.",
                        equipmentName));
    }

    /**
     * ACCESS REQUEST REJECTED — Notify the requester.
     */
    public void notifyAccessRequestRejected(UUID requesterId, UUID requestId, String equipmentName) {
        send(requesterId, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("Your access request for '%s' has been REJECTED. Contact the institution admin for details.",
                        equipmentName));
    }

    /**
     * WAITLIST FULFILLED — Notify user they got a slot from the waitlist.
     */
    public void notifyWaitlistFulfilled(UUID userId, UUID waitlistId, String equipmentName) {
        send(userId, NotificationReferenceType.WAITLIST, waitlistId,
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
        if (users == null) return List.of();
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

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .content(notification.getContent())
                .channel(notification.getChannel())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .status(notification.getStatus())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .title(notification.getReferenceType().name())
                .message(notification.getContent())
                .build();
    }
}
