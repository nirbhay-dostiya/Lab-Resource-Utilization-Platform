package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.Transaction;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Fetch all payment transactions applied to a specific invoice
    List<Transaction> findByInvoiceIdOrderByTransactionDateDesc(UUID invoiceId);

    // Find transactions by their status (e.g., to reconcile 'FAILED' payments)
    List<Transaction> findByStatus(TransactionStatus status);
}