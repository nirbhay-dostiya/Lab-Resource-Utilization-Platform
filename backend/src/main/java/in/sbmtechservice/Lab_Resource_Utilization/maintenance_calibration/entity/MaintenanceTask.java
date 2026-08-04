package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenancePriority;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Work Order / Maintenance Task entity.
 * Lifecycle enforced via state-machine in MaintenanceTaskService.
 */
@Entity
@Table(name = "maintenance_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MaintenanceTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /** The asset this work order is raised against. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    /** Assigned technician (may be null until ASSIGNED state). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    /** The user who raised / requested this work order. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 50)
    private MaintenanceType maintenanceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.CREATED;

    /** Work order priority — drives urgency coloring on Kanban board. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * Total downtime in hours caused by this maintenance event.
     * Used for OEE / idle-cost calculations in Module 5.
     */
    @Column(name = "downtime_hours", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal downtimeHours = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}