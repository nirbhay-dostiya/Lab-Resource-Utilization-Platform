package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.PaymentMethod;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private TransactionStatus status;
}
