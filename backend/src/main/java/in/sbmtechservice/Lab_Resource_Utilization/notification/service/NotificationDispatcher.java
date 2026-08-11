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
 * Audience constraint summary:
 *  - PROFILE events    → only the affected user (self-only, never to others)
 *  - INSTITUTION events → only Inst Admins of THAT institution + System Admins
 *  - DEPARTMENT events  → Inst Admins + Dept Heads of THAT institution
 *  - CATEGORY events    → System Admins only (global entity)
 *  - REPORT events      → only the requester
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
    public void send(UUID recipientId,
                     NotificationReferenceType type,
                     UUID referenceId,
                     String content) {
        sendWithActor(recipientId, type, referenceId, content, null, null, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWithActor(UUID recipientId,
                              NotificationReferenceType type,
                              UUID referenceId,
                              String content,
                              UUID actorId,
                              String actorName,
                              String action) {
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
                    .actorId(actorId)
                    .actorName(actorName)
                    .action(action)
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
    public void sendToAll(Collection<User> recipients,
                          NotificationReferenceType type,
                          UUID referenceId,
                          String content) {
        sendToAllWithActor(recipients, type, referenceId, content, null, null, null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToAllWithActor(Collection<User> recipients,
                                   NotificationReferenceType type,
                                   UUID referenceId,
                                   String content,
                                   UUID actorId,
                                   String actorName,
                                   String action) {
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
                    .actorId(actorId)
                    .actorName(actorName)
                    .action(action)
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

    /** Collect all system admins (global role, no institution scope). */
    public List<User> getSystemAdmins() {
        return userRepository.findAllByRoleName(RoleType.SYSTEM_ADMIN);
    }

    /** Collect all institution admins for a given institution. */
    public List<User> getInstAdmins(UUID institutionId) {
        if (institutionId == null) return List.of();
        return userRepository.findByRoleAndInstitutionId(RoleType.INSTITUTION_ADMIN, institutionId);
    }

    /** Collect all department heads for a given institution. */
    public List<User> getDeptHeads(UUID institutionId) {
        if (institutionId == null) return List.of();
        return userRepository.findByRoleAndInstitutionId(RoleType.DEPT_HEAD, institutionId);
    }

    /** Collect all lab managers for a given institution. */
    public List<User> getLabManagers(UUID institutionId) {
        if (institutionId == null) return List.of();
        return userRepository.findByRoleAndInstitutionId(RoleType.LAB_MANAGER, institutionId);
    }

    /** Collect all department heads for a specific department. */
    public List<User> getDeptHeadsForDept(UUID departmentId) {
        if (departmentId == null) return List.of();
        return userRepository.findByRoleAndDepartmentId(RoleType.DEPT_HEAD, departmentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  High-level domain-aware notification methods
    //  (called from business services — keeps business services clean)
    // ─────────────────────────────────────────────────────────────────────────

    // ── Equipment ─────────────────────────────────────────────────────────────

    /**
     * EQUIPMENT ADDED — Notify Dept Heads + Inst Admins + System Admins.
     * Actor (adder) gets a self-confirmation.
     */
    public void notifyEquipmentAdded(UUID institutionId, UUID equipmentId,
                                     String equipmentName, UUID addedById, String addedByName) {
        sendWithActor(addedById, NotificationReferenceType.EQUIPMENT, equipmentId,
                String.format("✅ You have successfully added equipment '%s'.", equipmentName),
                addedById, addedByName, "EQUIPMENT_ADDED");

        String msg = String.format("🔬 New equipment '%s' has been added to your institution by %s.",
                equipmentName, addedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        recipients.addAll(getSystemAdmins());
        recipients.removeIf(u -> u.getId().equals(addedById));

        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.EQUIPMENT, equipmentId, msg,
                addedById, addedByName, "EQUIPMENT_ADDED");
    }

    /**
     * EQUIPMENT UPDATED — Notify Dept Heads + Inst Admins.
     */
    public void notifyEquipmentUpdated(UUID institutionId, UUID equipmentId,
                                       String equipmentName, String updatedByName) {
        String msg = String.format("✏️ Equipment '%s' details have been updated by %s.",
                equipmentName, updatedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.EQUIPMENT, equipmentId, msg,
                null, updatedByName, "EQUIPMENT_UPDATED");
    }

    /**
     * EQUIPMENT STATUS CHANGED — Notify Dept Heads + Inst Admins.
     */
    public void notifyEquipmentStatusChanged(UUID institutionId, UUID equipmentId,
                                             String equipmentName, String oldStatus,
                                             String newStatus, String changedByName) {
        String msg = String.format("🔄 Equipment '%s' status changed from %s → %s by %s.",
                equipmentName, oldStatus, newStatus, changedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.EQUIPMENT, equipmentId, msg,
                null, changedByName, "STATUS_CHANGED");
    }

    // ── Booking ───────────────────────────────────────────────────────────────

    /**
     * BOOKING CREATED (PENDING) — Notify Lab Managers + Dept Heads + Inst Admin for approval.
     */
    public void notifyBookingPendingApproval(UUID equipmentInstitutionId, UUID bookingId,
                                             String bookerName, String equipmentName,
                                             String startTime) {
        String msg = String.format("⏳ New booking request from %s for '%s' starting %s is awaiting your approval.",
                bookerName, equipmentName, startTime);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getLabManagers(equipmentInstitutionId));
        recipients.addAll(getDeptHeads(equipmentInstitutionId));
        recipients.addAll(getInstAdmins(equipmentInstitutionId));
        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.BOOKING_APPROVAL_REQUEST, bookingId, msg,
                null, bookerName, "BOOKING_REQUESTED");
    }

    /**
     * BOOKING CREATED (PENDING_PAYMENT) — Notify the booker to pay invoice.
     */
    public void notifyBookingInvoiceGenerated(UUID bookerId, UUID bookingId,
                                              String equipmentName, String amount) {
        String msg = String.format(
                "📄 Your booking for '%s' has been created. Invoice of ₹%s has been generated. Please complete payment.",
                equipmentName, amount);
        sendWithActor(bookerId, NotificationReferenceType.INVOICE, bookingId, msg,
                null, null, "INVOICE_GENERATED");
    }

    /**
     * BOOKING CONFIRMED — Notify the booker.
     */
    public void notifyBookingConfirmed(UUID bookerId, UUID bookingId,
                                       String equipmentName, String startTime) {
        String msg = String.format(
                "✅ Your booking for '%s' on %s has been CONFIRMED. You're all set!",
                equipmentName, startTime);
        sendWithActor(bookerId, NotificationReferenceType.BOOKING, bookingId, msg,
                null, null, "BOOKING_CONFIRMED");
    }

    /**
     * BOOKING CANCELLED — Notify the booker + Dept Heads / Lab Managers of equipment's institution.
     */
    public void notifyBookingCancelled(UUID bookerId, UUID equipmentInstitutionId,
                                       UUID bookingId, String equipmentName,
                                       String cancelledByName) {
        sendWithActor(bookerId, NotificationReferenceType.BOOKING, bookingId,
                String.format("❌ Your booking for '%s' has been CANCELLED by %s.", equipmentName, cancelledByName),
                null, cancelledByName, "BOOKING_CANCELLED");

        List<User> staff = new ArrayList<>();
        staff.addAll(getLabManagers(equipmentInstitutionId));
        staff.addAll(getDeptHeads(equipmentInstitutionId));
        staff.removeIf(u -> u.getId().equals(bookerId));
        String staffMsg = String.format("❌ Booking for '%s' has been cancelled by %s.", equipmentName, cancelledByName);
        sendToAllWithActor(deduplicate(staff), NotificationReferenceType.BOOKING, bookingId, staffMsg,
                null, cancelledByName, "BOOKING_CANCELLED");
    }

    /**
     * BOOKING COMPLETED — Notify the booker.
     */
    public void notifyBookingCompleted(UUID bookerId, UUID bookingId, String equipmentName) {
        String msg = String.format(
                "✅ Your booking session for '%s' has been marked as COMPLETED. Thank you!",
                equipmentName);
        sendWithActor(bookerId, NotificationReferenceType.BOOKING, bookingId, msg,
                null, null, "BOOKING_COMPLETED");
    }

    // ── Maintenance & Work Orders ─────────────────────────────────────────────

    /**
     * MAINTENANCE SCHEDULED — Notify technician + Dept Heads + Inst Admins.
     */
    public void notifyMaintenanceScheduled(UUID technicianId, UUID institutionId,
                                           UUID taskId, String equipmentName,
                                           String scheduledDate) {
        if (technicianId != null) {
            sendWithActor(technicianId, NotificationReferenceType.MAINTENANCE, taskId,
                    String.format("🔧 You have been assigned a maintenance task for '%s' on %s.",
                            equipmentName, scheduledDate),
                    null, null, "MAINTENANCE_SCHEDULED");
        }
        String mgmtMsg = String.format(
                "🔧 Maintenance has been scheduled for '%s' on %s. Equipment is now locked.",
                equipmentName, scheduledDate);
        List<User> mgmt = new ArrayList<>();
        mgmt.addAll(getDeptHeads(institutionId));
        mgmt.addAll(getInstAdmins(institutionId));
        sendToAllWithActor(deduplicate(mgmt), NotificationReferenceType.MAINTENANCE, taskId, mgmtMsg,
                null, null, "MAINTENANCE_SCHEDULED");
    }

    /**
     * MAINTENANCE COMPLETED — Notify Dept Heads + Inst Admins.
     */
    public void notifyMaintenanceCompleted(UUID institutionId, UUID taskId, String equipmentName) {
        String msg = String.format(
                "✅ Maintenance for '%s' has been COMPLETED. Equipment is now back online and available.",
                equipmentName);
        List<User> mgmt = new ArrayList<>();
        mgmt.addAll(getDeptHeads(institutionId));
        mgmt.addAll(getInstAdmins(institutionId));
        sendToAllWithActor(deduplicate(mgmt), NotificationReferenceType.MAINTENANCE, taskId, msg,
                null, null, "MAINTENANCE_COMPLETED");
    }

    /**
     * WORK ORDER STATUS CHANGED — Notify technician (if assigned) + Dept Heads + Inst Admins.
     * Fires on every valid state transition (ASSIGNED, IN_PROGRESS, COMPLETED, VERIFIED, CANCELLED).
     */
    public void notifyWorkOrderStatusChanged(UUID institutionId, UUID technicianId,
                                             UUID workOrderId, String equipmentName,
                                             String newStatus, UUID changedById, String changedByName) {
        String techMsg = String.format("🔧 Work order status for '%s' has been changed to %s.",
                equipmentName, newStatus);
        if (technicianId != null && !technicianId.equals(changedById)) {
            sendWithActor(technicianId, NotificationReferenceType.MAINTENANCE, workOrderId,
                    techMsg, changedById, changedByName, "WORK_ORDER_STATUS_CHANGED");
        }

        String mgmtMsg = String.format("🔧 Work order for '%s' transitioned to %s by %s.",
                equipmentName, newStatus, changedByName);
        List<User> mgmt = new ArrayList<>();
        mgmt.addAll(getDeptHeads(institutionId));
        mgmt.addAll(getInstAdmins(institutionId));
        mgmt.removeIf(u -> u.getId().equals(changedById));
        sendToAllWithActor(deduplicate(mgmt), NotificationReferenceType.MAINTENANCE, workOrderId,
                mgmtMsg, changedById, changedByName, "WORK_ORDER_STATUS_CHANGED");

        // Self-confirm to the actor
        sendWithActor(changedById, NotificationReferenceType.MAINTENANCE, workOrderId,
                String.format("✅ You changed the work order status for '%s' to %s.", equipmentName, newStatus),
                changedById, changedByName, "WORK_ORDER_STATUS_CHANGED");
    }

    // ── Calibration ───────────────────────────────────────────────────────────

    /**
     * CALIBRATION LOGGED — Notify Lab Managers + Dept Heads of the equipment's institution.
     * Actor gets a self-confirmation.
     */
    public void notifyCalibrationLogged(UUID institutionId, UUID calibrationId,
                                        String equipmentName, String expiryDate,
                                        UUID loggedById, String loggedByName) {
        sendWithActor(loggedById, NotificationReferenceType.CALIBRATION, calibrationId,
                String.format("✅ You have successfully logged a calibration record for '%s'. Next due: %s.",
                        equipmentName, expiryDate),
                loggedById, loggedByName, "CALIBRATION_LOGGED");

        String msg = String.format("📋 Calibration record for '%s' has been logged by %s. Next due: %s.",
                equipmentName, loggedByName, expiryDate);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getLabManagers(institutionId));
        recipients.addAll(getDeptHeads(institutionId));
        recipients.addAll(getInstAdmins(institutionId));
        recipients.removeIf(u -> u.getId().equals(loggedById));
        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.CALIBRATION, calibrationId, msg,
                loggedById, loggedByName, "CALIBRATION_LOGGED");
    }

    // ── Institution ───────────────────────────────────────────────────────────

    /**
     * INSTITUTION APPROVED — Notify Institution Admins of that institution + System Admins.
     * Audience: strictly scoped to the approved institution's admins; no other institution sees this.
     */
    public void notifyInstitutionApproved(UUID institutionId, String institutionName) {
        String instAdminMsg = String.format(
                "🎉 Your institution '%s' has been APPROVED! Your account is now fully active.", institutionName);
        getInstAdmins(institutionId).forEach(u ->
                sendWithActor(u.getId(), NotificationReferenceType.INSTITUTION, institutionId,
                        instAdminMsg, null, "System", "INSTITUTION_APPROVED"));

        String sysAdminMsg = String.format("✅ Institution '%s' has been approved and activated.", institutionName);
        sendToAllWithActor(getSystemAdmins(), NotificationReferenceType.INSTITUTION, institutionId,
                sysAdminMsg, null, "System", "INSTITUTION_APPROVED");
    }

    /**
     * INSTITUTION SUSPENDED — Notify Institution Admins of that institution + System Admins.
     * Audience: strictly scoped to the suspended institution's admins; no other institution sees this.
     */
    public void notifyInstitutionSuspended(UUID institutionId, String institutionName) {
        String instAdminMsg = String.format(
                "⚠️ Your institution '%s' has been SUSPENDED. Please contact the system administrator.", institutionName);
        getInstAdmins(institutionId).forEach(u ->
                sendWithActor(u.getId(), NotificationReferenceType.INSTITUTION, institutionId,
                        instAdminMsg, null, "System", "INSTITUTION_SUSPENDED"));

        String sysAdminMsg = String.format("⚠️ Institution '%s' has been suspended.", institutionName);
        sendToAllWithActor(getSystemAdmins(), NotificationReferenceType.INSTITUTION, institutionId,
                sysAdminMsg, null, "System", "INSTITUTION_SUSPENDED");
    }

    // ── Department ────────────────────────────────────────────────────────────

    /**
     * DEPARTMENT CREATED — Notify Inst Admins + Dept Heads of the same institution.
     * Creator gets a self-confirmation. Scoped to the institution.
     */
    public void notifyDepartmentCreated(UUID institutionId, UUID departmentId,
                                        String departmentName, UUID createdById, String createdByName) {
        sendWithActor(createdById, NotificationReferenceType.DEPARTMENT, departmentId,
                String.format("✅ You have successfully created department '%s'.", departmentName),
                createdById, createdByName, "DEPARTMENT_CREATED");

        String msg = String.format("🏢 New department '%s' has been created by %s.", departmentName, createdByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getInstAdmins(institutionId));
        recipients.addAll(getDeptHeads(institutionId));
        recipients.removeIf(u -> u.getId().equals(createdById));
        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.DEPARTMENT, departmentId, msg,
                createdById, createdByName, "DEPARTMENT_CREATED");
    }

    /**
     * DEPARTMENT UPDATED — Notify Inst Admins + Dept Heads of the same institution.
     */
    public void notifyDepartmentUpdated(UUID institutionId, UUID departmentId,
                                        String departmentName, UUID updatedById, String updatedByName) {
        String msg = String.format("✏️ Department '%s' has been updated by %s.", departmentName, updatedByName);
        List<User> recipients = new ArrayList<>();
        recipients.addAll(getInstAdmins(institutionId));
        recipients.addAll(getDeptHeads(institutionId));
        sendToAllWithActor(deduplicate(recipients), NotificationReferenceType.DEPARTMENT, departmentId, msg,
                updatedById, updatedByName, "DEPARTMENT_UPDATED");
    }

    // ── Category ──────────────────────────────────────────────────────────────

    /**
     * CATEGORY ADDED — Notify System Admins only (categories are global entities).
     * Actor gets a self-confirmation.
     */
    public void notifyCategoryAdded(UUID categoryId, String categoryName,
                                    UUID addedById, String addedByName) {
        sendWithActor(addedById, NotificationReferenceType.CATEGORY, categoryId,
                String.format("✅ You have successfully added equipment category '%s'.", categoryName),
                addedById, addedByName, "CATEGORY_ADDED");

        String sysMsg = String.format("🏷️ New equipment category '%s' has been added by %s.", categoryName, addedByName);
        List<User> sysAdmins = getSystemAdmins();
        sysAdmins.removeIf(u -> u.getId().equals(addedById));
        sendToAllWithActor(sysAdmins, NotificationReferenceType.CATEGORY, categoryId,
                sysMsg, addedById, addedByName, "CATEGORY_ADDED");
    }

    // ── User & Profile ────────────────────────────────────────────────────────

    /**
     * PROFILE UPDATED — SELF ONLY.
     * Audience constraint: ONLY the user whose profile was changed receives this.
     * No admin, no other user, no institution-wide broadcast. Ever.
     */
    public void notifyProfileUpdated(UUID userId, String userName) {
        sendWithActor(userId, NotificationReferenceType.PROFILE, userId,
                String.format("✅ Your profile has been updated successfully, %s.", userName),
                userId, userName, "PROFILE_UPDATED");
    }

    /**
     * USER CREATED — Notify the new user (welcome) + Institution Admins of their institution.
     */
    public void notifyUserCreated(UUID institutionId, UUID newUserId,
                                  String newUserName, String newUserEmail,
                                  UUID createdById, String createdByName) {
        sendWithActor(newUserId, NotificationReferenceType.USER_MANAGEMENT, newUserId,
                String.format("👋 Welcome, %s! Your account has been created. You can now log in with %s.",
                        newUserName, newUserEmail),
                createdById, createdByName, "USER_CREATED");

        String adminMsg = String.format("👤 New user '%s' (%s) has been created by %s.",
                newUserName, newUserEmail, createdByName);
        List<User> admins = new ArrayList<>(getInstAdmins(institutionId));
        admins.addAll(getSystemAdmins());
        admins.removeIf(u -> u.getId().equals(createdById));
        sendToAllWithActor(deduplicate(admins), NotificationReferenceType.USER_MANAGEMENT, newUserId,
                adminMsg, createdById, createdByName, "USER_CREATED");
    }

    /**
     * USER STATUS TOGGLED — Notify the affected user + Institution Admins of their institution.
     */
    public void notifyUserStatusToggled(UUID institutionId, UUID userId, String userName,
                                        boolean isNowActive, UUID adminId, String adminName) {
        String status = isNowActive ? "ACTIVATED ✅" : "SUSPENDED ⚠️";
        sendWithActor(userId, NotificationReferenceType.USER_MANAGEMENT, userId,
                String.format("Your account has been %s by %s.", status, adminName),
                adminId, adminName, isNowActive ? "USER_ACTIVATED" : "USER_SUSPENDED");

        String adminMsg = String.format("👤 User '%s' has been %s by %s.", userName, status, adminName);
        List<User> admins = new ArrayList<>(getInstAdmins(institutionId));
        admins.addAll(getSystemAdmins());
        admins.removeIf(u -> u.getId().equals(adminId));
        sendToAllWithActor(deduplicate(admins), NotificationReferenceType.USER_MANAGEMENT, userId,
                adminMsg, adminId, adminName, isNowActive ? "USER_ACTIVATED" : "USER_SUSPENDED");
    }

    /**
     * USER ROLE ASSIGNED — Notify the affected user + Institution Admins of their institution.
     */
    public void notifyUserRoleAssigned(UUID institutionId, UUID userId, String userName,
                                       String roleName, UUID adminId, String adminName) {
        sendWithActor(userId, NotificationReferenceType.USER_MANAGEMENT, userId,
                String.format("🎖️ The role '%s' has been assigned to your account by %s.", roleName, adminName),
                adminId, adminName, "ROLE_ASSIGNED");

        String adminMsg = String.format("🎖️ Role '%s' has been assigned to '%s' by %s.", roleName, userName, adminName);
        List<User> admins = new ArrayList<>(getInstAdmins(institutionId));
        admins.addAll(getSystemAdmins());
        admins.removeIf(u -> u.getId().equals(adminId));
        sendToAllWithActor(deduplicate(admins), NotificationReferenceType.USER_MANAGEMENT, userId,
                adminMsg, adminId, adminName, "ROLE_ASSIGNED");
    }

    // ── Reporting ─────────────────────────────────────────────────────────────

    /**
     * OEE REPORT READY — REQUESTER ONLY.
     * Audience constraint: Only the user who triggered the report generation is notified.
     * No broadcast to admins or other users.
     */
    public void notifyOeeReportReady(UUID requesterId, UUID reportId, String reportName) {
        sendWithActor(requesterId, NotificationReferenceType.REPORT, reportId,
                String.format("📊 Your OEE report '%s' has been generated and is ready to view.", reportName),
                null, "System", "REPORT_READY");
    }

    // ── Resource Sharing ──────────────────────────────────────────────────────

    /**
     * RESOURCE SHARE LISTED — Notify the person who listed it + Dept Heads + Inst Admins + System Admins.
     */
    public void notifyResourceShareListed(UUID listingId, String equipmentName, String institutionName,
                                          UUID sharedById, UUID institutionId) {
        sendWithActor(sharedById, NotificationReferenceType.SHARING_REQUEST, listingId,
                String.format("🔗 You have successfully listed '%s' for inter-institution sharing.", equipmentName),
                sharedById, null, "SHARE_LISTED");

        String staffMsg = String.format("🔗 Equipment '%s' has been listed for inter-institution sharing.", equipmentName);
        List<User> staff = new ArrayList<>();
        staff.addAll(getDeptHeads(institutionId));
        staff.addAll(getInstAdmins(institutionId));
        staff.removeIf(u -> u.getId().equals(sharedById));
        sendToAllWithActor(deduplicate(staff), NotificationReferenceType.SHARING_REQUEST, listingId, staffMsg,
                sharedById, null, "SHARE_LISTED");

        String sysAdminMsg = String.format("🔗 '%s' from %s has been listed for inter-institution sharing.",
                equipmentName, institutionName);
        sendToAllWithActor(getSystemAdmins(), NotificationReferenceType.SHARING_REQUEST, listingId, sysAdminMsg,
                sharedById, null, "SHARE_LISTED");
    }

    /**
     * ACCESS REQUEST SUBMITTED — Notify owning institution's staff + requester.
     */
    public void notifyAccessRequestSubmitted(UUID ownerInstitutionId, UUID requestId,
                                             String requesterName, String equipmentName,
                                             String requesterInstitutionName, UUID requesterId) {
        sendWithActor(requesterId, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("✅ Your access request for '%s' has been successfully submitted.", equipmentName),
                requesterId, requesterName, "ACCESS_REQUESTED");

        String msg = String.format("🔑 %s from %s has requested access to your shared equipment '%s'. Please review.",
                requesterName, requesterInstitutionName, equipmentName);
        List<User> staff = new ArrayList<>();
        staff.addAll(getLabManagers(ownerInstitutionId));
        staff.addAll(getDeptHeads(ownerInstitutionId));
        staff.addAll(getInstAdmins(ownerInstitutionId));
        staff.removeIf(u -> u.getId().equals(requesterId));
        sendToAllWithActor(deduplicate(staff), NotificationReferenceType.ACCESS_REQUEST, requestId, msg,
                requesterId, requesterName, "ACCESS_REQUESTED");
    }

    /**
     * ACCESS REQUEST APPROVED — Notify the requester.
     */
    public void notifyAccessRequestApproved(UUID requesterId, UUID requestId, String equipmentName) {
        sendWithActor(requesterId, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("✅ Your access request for '%s' has been APPROVED. You can now book this equipment.",
                        equipmentName),
                null, null, "ACCESS_APPROVED");
    }

    /**
     * ACCESS REQUEST REJECTED — Notify the requester.
     */
    public void notifyAccessRequestRejected(UUID requesterId, UUID requestId, String equipmentName) {
        sendWithActor(requesterId, NotificationReferenceType.ACCESS_REQUEST, requestId,
                String.format("❌ Your access request for '%s' has been REJECTED. Contact the institution admin for details.",
                        equipmentName),
                null, null, "ACCESS_REJECTED");
    }

    /**
     * WAITLIST FULFILLED — Notify user they got a slot.
     */
    public void notifyWaitlistFulfilled(UUID userId, UUID waitlistId, String equipmentName) {
        sendWithActor(userId, NotificationReferenceType.WAITLIST, waitlistId,
                String.format("🎉 A slot for '%s' became available! Your waitlist request has been fulfilled and a new booking created for you.",
                        equipmentName),
                null, null, "WAITLIST_FULFILLED");
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
                .actorId(notification.getActorId())
                .actorName(notification.getActorName())
                .action(notification.getAction())
                .build();
    }
}
