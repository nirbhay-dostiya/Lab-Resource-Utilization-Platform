package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.Invoice;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // Fetch all invoices billed to a specific external institution
    List<Invoice> findByBilledToInstitutionIdOrderByInvoiceDateDesc(UUID institutionId);

    // Fetch all invoices (internal chargebacks) billed to a specific department
    List<Invoice> findByBilledToDepartmentIdOrderByInvoiceDateDesc(UUID departmentId);

    // Find invoices by their current status (e.g., to view all 'PAID' or 'DRAFT' invoices)
    List<Invoice> findByStatus(InvoiceStatus status);

    // OVERDUE LOGIC: Find invoices that are not paid and have passed their due date
    @Query("SELECT i FROM Invoice i WHERE i.status NOT IN ('PAID', 'CANCELLED') AND i.dueDate < :currentDate")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDate currentDate);

    /**
     * Finds all invoices where a given institution is involved as EITHER:
     *  1. The PAYER:
     *     - Directly billed to the institution (billedToInstitution)
     *     - Billed to a department that belongs to the institution (billedToDepartment.institution)
     *  2. The PROVIDER:
     *     - Has a booking line item referencing a booking for equipment owned by the institution
     *
     * Note: referenceId is a loose UUID (not a JPA relationship), so we use a native-style
     * subquery via the Booking entity to find provider-side invoices.
     */
    @Query("SELECT DISTINCT i FROM Invoice i " +
           "LEFT JOIN i.billedToInstitution bti " +
           "LEFT JOIN i.billedToDepartment btd " +
           "LEFT JOIN btd.institution btdi " +
           "WHERE bti.id = :institutionId " +
           "   OR btdi.id = :institutionId " +
           "   OR EXISTS (" +
           "       SELECT 1 FROM InvoiceLineItem li " +
           "       JOIN Booking b ON b.id = li.referenceId " +
           "       JOIN b.equipment eq " +
           "       JOIN eq.department eqd " +
           "       WHERE li.invoice = i " +
           "         AND li.referenceType = 'BOOKING' " +
           "         AND eqd.institution.id = :institutionId" +
           "   ) " +
           "ORDER BY i.invoiceDate DESC")
    List<Invoice> findInvoicesByInstitutionInvolved(@Param("institutionId") UUID institutionId);
}