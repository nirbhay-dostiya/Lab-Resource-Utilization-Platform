package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity;

import in.sbmtechservice.Lab_Resource_Utilization.inventory.entity.Equipment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "shared_equipment_listings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agreement_id", "equipment_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SharedEquipmentListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // Many-to-One relationship with the Sharing Agreement
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private SharingAgreement agreement;

    // Many-to-One relationship with Equipment (The shared asset)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "external_hourly_rate", precision = 10, scale = 2, nullable = false)
    private BigDecimal externalHourlyRate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Inverse side of the One-to-Many relationship with Access Requests
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<AccessRequest> accessRequests = new HashSet<>();
}