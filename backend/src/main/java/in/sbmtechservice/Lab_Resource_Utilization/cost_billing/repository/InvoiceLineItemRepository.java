package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    // Fetch all line items for a specific invoice
    List<InvoiceLineItem> findByInvoiceId(UUID invoiceId);

    // Fetch line items based on the reference (e.g., find the line item for a specific booking)
    List<InvoiceLineItem> findByReferenceId(UUID referenceId);
}