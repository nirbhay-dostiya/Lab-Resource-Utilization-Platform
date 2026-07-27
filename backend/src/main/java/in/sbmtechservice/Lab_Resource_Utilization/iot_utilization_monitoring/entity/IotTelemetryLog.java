package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.enums.SensorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "iot_telemetry_logs",
        indexes = {
                // Crucial for time-series performance when querying historical data
                @Index(name = "idx_telemetry_equip_time", columnList = "equipment_id, recorded_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IotTelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // Many-to-One ONLY. (Do not put mappedBy in Equipment).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_status", nullable = false, length = 50)
    private SensorStatus sensorStatus;

    @Column(name = "reading_value", precision = 10, scale = 2)
    private BigDecimal readingValue;

    @CreationTimestamp
    @Column(name = "ingested_at", nullable = false, updatable = false)
    private LocalDateTime ingestedAt;
}