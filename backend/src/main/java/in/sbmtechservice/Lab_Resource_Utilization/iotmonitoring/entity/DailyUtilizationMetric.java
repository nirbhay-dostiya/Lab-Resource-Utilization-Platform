package in.sbmtechservice.Lab_Resource_Utilization.iotmonitoring.entity;

import in.sbmtechservice.Lab_Resource_Utilization.inventory.entity.Equipment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "daily_utilization_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"equipment_id", "record_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DailyUtilizationMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // Many-to-One ONLY. (Do not put mappedBy in Equipment).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "total_booked_minutes", nullable = false)
    @Builder.Default
    private Integer totalBookedMinutes = 0;

    @Column(name = "total_used_minutes", nullable = false)
    @Builder.Default
    private Integer totalUsedMinutes = 0;

    @Column(name = "utilization_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal utilizationRate = BigDecimal.ZERO;

    @Column(name = "idle_time_minutes", nullable = false)
    @Builder.Default
    private Integer idleTimeMinutes = 0;

    @UpdateTimestamp
    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;
}