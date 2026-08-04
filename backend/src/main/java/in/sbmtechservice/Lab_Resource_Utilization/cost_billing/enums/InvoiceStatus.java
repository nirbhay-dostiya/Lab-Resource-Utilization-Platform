package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums;

/**
 * Full billing approval lifecycle:
 *
 * DRAFT → PENDING_APPROVAL → APPROVED → ISSUED → PAID
 *                                             ↘ OVERDUE
 * Any stage → CANCELLED
 */
public enum InvoiceStatus {
    /** Invoice is being composed — not yet submitted for approval. */
    DRAFT,
    /** Submitted and awaiting manager/admin sign-off. */
    PENDING_APPROVAL,
    /** Approved by authorized manager — ready to be issued/sent. */
    APPROVED,
    /** Formally issued to the billed party. */
    ISSUED,
    /** Payment received. Terminal state. */
    PAID,
    /** Past due date without payment. */
    OVERDUE,
    /** Cancelled at any stage. Terminal state. */
    CANCELLED
}
