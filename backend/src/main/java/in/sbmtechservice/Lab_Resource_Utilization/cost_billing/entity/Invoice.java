package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // External billing target
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billed_to_institution_id")
    private Institution billedToInstitution;

    // Internal chargeback target
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billed_to_department_id")
    private Department billedToDepartment;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InvoiceStatus status;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    /**
     * Overhead/tax rate applied to inter-institutional invoices.
     * e.g., 0.25 = 25% overhead surcharge for external institutions.
     */
    @Column(name = "overhead_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal overheadRate = BigDecimal.ZERO;

    /**
     * The funding source linked to this invoice (grant/PO number, institution origin).
     * Null for internal department chargebacks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_source_id")
    private FundingSource fundingSource;

    /**
     * User who approved this invoice (LAB_MANAGER / DEPT_HEAD / INSTITUTION_ADMIN).
     * Null until moved to APPROVED state.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Inverse side of the One-to-Many relationship with Line Items
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<InvoiceLineItem> lineItems = new HashSet<>();

    // Inverse side of the One-to-Many relationship with Transactions
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Transaction> transactions = new HashSet<>();

    // Validation Method: Ensure only one billing target is set
    @PrePersist
    @PreUpdate
    private void validateBillingTarget() {
        if ((billedToInstitution == null && billedToDepartment == null) ||
                (billedToInstitution != null && billedToDepartment != null)) {
            throw new IllegalStateException("An invoice must be billed to EXACTLY ONE target: either an Institution OR a Department.");
        }
    }
}