package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.MaintenanceTask;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenancePriority;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository.MaintenanceTaskRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Work Order / Maintenance Task Service with enforced State Machine.
 *
 * <pre>
 * Valid transitions:
 *   CREATED     → ASSIGNED    (LAB_MANAGER, DEPT_HEAD, SYSTEM_ADMIN)
 *   ASSIGNED    → IN_PROGRESS (LAB_TECHNICIAN, LAB_MANAGER)
 *   IN_PROGRESS → COMPLETED   (LAB_TECHNICIAN, LAB_MANAGER)
 *   COMPLETED   → VERIFIED    (LAB_MANAGER, DEPT_HEAD, SYSTEM_ADMIN — final sign-off)
 *   *           → CANCELLED   (LAB_MANAGER, SYSTEM_ADMIN)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {

    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public MaintenanceTaskResponse scheduleTask(MaintenanceTaskRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found: " + request.getEquipmentId()));

        User technician = null;
        if (request.getTechnicianId() != null) {
            technician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new IllegalArgumentException("Technician not found: " + request.getTechnicianId()));
        }

        User requester = null;
        if (request.getRequesterId() != null) {
            requester = userRepository.findById(request.getRequesterId())
                    .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + request.getRequesterId()));
        }

        MaintenanceTask task = MaintenanceTask.builder()
                .equipment(equipment)
                .technician(technician)
                .requester(requester)
                .maintenanceType(request.getMaintenanceType())
                .priority(request.getPriority() != null ? request.getPriority() : MaintenancePriority.MEDIUM)
                .scheduledDate(request.getScheduledDate())
                .description(request.getDescription())
                .cost(request.getEstimatedCost() != null ? request.getEstimatedCost() : BigDecimal.ZERO)
                .downtimeHours(request.getDowntimeHours() != null ? request.getDowntimeHours() : BigDecimal.ZERO)
                .status(MaintenanceStatus.CREATED)
                .build();

        // Lock the equipment — flag as UNAVAILABLE for booking system
        equipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
        equipmentRepository.save(equipment);

        // Cancel any conflicting CONFIRMED or IN_USE bookings
        cancelConflictingBookings(equipment, request.getScheduledDate());

        MaintenanceTask saved = maintenanceTaskRepository.save(task);

        // Publish domain event for notification consumers
        UUID institutionId = equipment.getDepartment().getInstitution().getId();
        String scheduledDateStr = request.getScheduledDate() != null ? request.getScheduledDate().toString() : "TBD";
        eventPublisher.publishEvent(new NotificationEvents.MaintenanceScheduledEvent(
                technician != null ? technician.getId() : null,
                institutionId,
                saved.getId(),
                equipment.getName(),
                scheduledDateStr
        ));

        log.info("[WORK-ORDER] Created work order {} for equipment '{}' with status CREATED",
                saved.getId(), equipment.getName());
        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATE MACHINE — transition
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Role-aware state transition method.
     * Throws IllegalStateException for invalid transitions or unauthorized roles.
     */
    @Transactional
    public MaintenanceTaskResponse transitionStatus(UUID taskId,
                                                    MaintenanceStatus targetStatus,
                                                    String performingUserRoles,
                                                    UUID performingUserId,
                                                    String resolutionNotes,
                                                    BigDecimal finalCost,
                                                    BigDecimal downtimeHours) {
        MaintenanceTask task = maintenanceTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Work Order not found: " + taskId));

        validateTransition(task.getStatus(), targetStatus, performingUserRoles);

        MaintenanceStatus previous = task.getStatus();
        task.setStatus(targetStatus);

        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            task.setResolutionNotes(resolutionNotes);
        }
        if (finalCost != null) {
            task.setCost(finalCost);
        }
        if (downtimeHours != null) {
            task.setDowntimeHours(downtimeHours);
        }

        // When moving to COMPLETED, record timestamp
        if (targetStatus == MaintenanceStatus.COMPLETED) {
            task.setCompletedDate(LocalDateTime.now());
        }

        // When VERIFIED or CANCELLED — restore equipment to AVAILABLE
        if (targetStatus == MaintenanceStatus.VERIFIED || targetStatus == MaintenanceStatus.CANCELLED) {
            Equipment equipment = task.getEquipment();
            equipment.setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(equipment);

            // Publish downtime event for analytics
            UUID institutionId = equipment.getDepartment().getInstitution().getId();
            eventPublisher.publishEvent(new NotificationEvents.MaintenanceCompletedEvent(
                    institutionId, taskId, equipment.getName()
            ));
            log.info("[WORK-ORDER] Equipment '{}' restored to AVAILABLE after transition to {}",
                    equipment.getName(), targetStatus);
        }

        // Urgent notification if priority escalated during transition
        if (task.getPriority() == MaintenancePriority.CRITICAL || task.getPriority() == MaintenancePriority.HIGH) {
            UUID institutionId = task.getEquipment().getDepartment().getInstitution().getId();
            eventPublisher.publishEvent(new NotificationEvents.WorkOrderUrgentEvent(
                    institutionId, taskId, task.getEquipment().getName(), task.getPriority().name(), targetStatus.name()
            ));
        }

        MaintenanceTask saved = maintenanceTaskRepository.save(task);
        log.info("[WORK-ORDER] Transitioned {} from {} → {} by user {}", taskId, previous, targetStatus, performingUserId);
        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Legacy method — kept for backward compatibility
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public MaintenanceTaskResponse completeTask(UUID taskId, String resolutionNotes, BigDecimal finalCost) {
        return transitionStatus(taskId, MaintenanceStatus.COMPLETED,
                "LAB_MANAGER", null, resolutionNotes, finalCost, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    public List<MaintenanceTaskResponse> getTasksByEquipment(UUID equipmentId) {
        return maintenanceTaskRepository.findByEquipmentIdOrderByScheduledDateDesc(equipmentId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<MaintenanceTaskResponse> getAllTasks() {
        return maintenanceTaskRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<MaintenanceTaskResponse> getTasksByStatus(MaintenanceStatus status) {
        return maintenanceTaskRepository.findByStatus(status)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<MaintenanceTaskResponse> getTasksByTechnician(UUID technicianId) {
        return maintenanceTaskRepository.findByTechnicianIdOrderByScheduledDateAsc(technicianId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * State machine transition validation table.
     * Throws {@link IllegalStateException} for invalid transitions or unauthorized actors.
     */
    private void validateTransition(MaintenanceStatus current, MaintenanceStatus target, String rolesStr) {
        Set<String> roles = rolesStr != null
                ? Set.of(rolesStr.split(","))
                : Set.of();

        boolean isAdmin     = roles.contains("SYSTEM_ADMIN");
        boolean isManager   = roles.contains("LAB_MANAGER");
        boolean isDeptHead  = roles.contains("DEPT_HEAD");
        boolean isTech      = roles.contains("LAB_TECHNICIAN");

        switch (target) {
            case ASSIGNED -> {
                if (current != MaintenanceStatus.CREATED)
                    throw new IllegalStateException("Can only ASSIGN a work order in CREATED state. Current: " + current);
                if (!isAdmin && !isManager && !isDeptHead)
                    throw new SecurityException("Only LAB_MANAGER, DEPT_HEAD, or SYSTEM_ADMIN can assign work orders.");
            }
            case IN_PROGRESS -> {
                if (current != MaintenanceStatus.ASSIGNED)
                    throw new IllegalStateException("Can only start IN_PROGRESS from ASSIGNED state. Current: " + current);
                if (!isAdmin && !isManager && !isTech)
                    throw new SecurityException("Only LAB_TECHNICIAN or LAB_MANAGER can start a work order.");
            }
            case COMPLETED -> {
                if (current != MaintenanceStatus.IN_PROGRESS)
                    throw new IllegalStateException("Can only COMPLETE from IN_PROGRESS state. Current: " + current);
                if (!isAdmin && !isManager && !isTech)
                    throw new SecurityException("Only LAB_TECHNICIAN or LAB_MANAGER can complete a work order.");
            }
            case VERIFIED -> {
                if (current != MaintenanceStatus.COMPLETED)
                    throw new IllegalStateException("Can only VERIFY a COMPLETED work order. Current: " + current);
                if (!isAdmin && !isManager && !isDeptHead)
                    throw new SecurityException("Only LAB_MANAGER, DEPT_HEAD, or SYSTEM_ADMIN can verify a work order.");
            }
            case CANCELLED -> {
                if (current == MaintenanceStatus.VERIFIED)
                    throw new IllegalStateException("Cannot cancel a VERIFIED work order.");
                if (!isAdmin && !isManager)
                    throw new SecurityException("Only LAB_MANAGER or SYSTEM_ADMIN can cancel a work order.");
            }
            default -> throw new IllegalArgumentException("Invalid target status: " + target);
        }
    }

    /**
     * Cancel all CONFIRMED / IN_USE bookings for equipment that overlap with maintenance window.
     * Runs within the calling transaction.
     */
    private void cancelConflictingBookings(Equipment equipment, LocalDateTime maintenanceStart) {
        if (maintenanceStart == null) return;
        List<Booking> conflicts = bookingRepository
                .findByEquipmentIdAndStatusInAndEndTimeAfter(
                        equipment.getId(),
                        List.of(BookingStatus.CONFIRMED, BookingStatus.IN_USE),
                        maintenanceStart
                );
        for (Booking b : conflicts) {
            b.setStatus(BookingStatus.CANCELLED);
            // Fire cancelled event for each booking owner
            eventPublisher.publishEvent(new NotificationEvents.BookingCancelledEvent(
                    b.getUser().getId(),
                    equipment.getDepartment().getInstitution().getId(),
                    b.getId(),
                    equipment.getName(),
                    "Maintenance System"
            ));
            log.info("[WORK-ORDER] Cancelled conflicting booking {} due to maintenance on '{}'",
                    b.getId(), equipment.getName());
        }
        bookingRepository.saveAll(conflicts);
    }

    public MaintenanceTaskResponse mapToResponse(MaintenanceTask task) {
        String techName = task.getTechnician() != null
                ? task.getTechnician().getFirstName() + " " + task.getTechnician().getLastName()
                : "Unassigned";
        String requesterName = task.getRequester() != null
                ? task.getRequester().getFirstName() + " " + task.getRequester().getLastName()
                : "N/A";

        return MaintenanceTaskResponse.builder()
                .id(task.getId())
                .equipmentId(task.getEquipment().getId())
                .equipmentName(task.getEquipment().getName())
                .technicianId(task.getTechnician() != null ? task.getTechnician().getId() : null)
                .technicianName(techName)
                .requesterId(task.getRequester() != null ? task.getRequester().getId() : null)
                .requesterName(requesterName)
                .maintenanceType(task.getMaintenanceType())
                .status(task.getStatus())
                .priority(task.getPriority())
                .scheduledDate(task.getScheduledDate())
                .completedDate(task.getCompletedDate())
                .description(task.getDescription())
                .resolutionNotes(task.getResolutionNotes())
                .downtimeHours(task.getDowntimeHours())
                .cost(task.getCost())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}