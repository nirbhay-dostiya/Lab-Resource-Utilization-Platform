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

    // 🚨 OVERDUE LOGIC: Find invoices that are not paid and have passed their due date 🚨
    @Query("SELECT i FROM Invoice i WHERE i.status NOT IN ('PAID', 'CANCELLED') AND i.dueDate < :currentDate")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDate currentDate);
}