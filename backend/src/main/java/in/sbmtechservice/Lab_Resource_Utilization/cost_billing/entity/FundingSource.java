package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity;

import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Funding Source entity — tracks grants, purchase orders, and external funding
 * attached to invoices, enabling inter-institutional billing rules.
 *
 * Key billing rule: if institutionOrigin.id != facility host institution id
 *   → apply Invoice.overheadRate multiplier to total amount.
 */
@Entity
@Table(name = "funding_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FundingSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Grant number (e.g., NIH-R01-12345). Nullable if PO-based. */
    @Column(name = "grant_number", length = 100)
    private String grantNumber;

    /** Purchase Order number. Nullable if grant-based. */
    @Column(name = "po_number", length = 100)
    private String poNumber;

    /** When this funding source expires — used for budget validity checks. */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * The institution that owns/provides this funding.
     * If this differs from the facility's host institution → overhead rate applies.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_origin_id")
    private Institution institutionOrigin;

    /** Total budget allocated under this funding source. */
    @Column(name = "total_budget", precision = 14, scale = 2)
    private java.math.BigDecimal totalBudget;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
