package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity;

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