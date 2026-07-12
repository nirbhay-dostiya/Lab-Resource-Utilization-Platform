package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.ReferenceType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {
    private UUID billedToInstitutionId; // Pass this OR Department, not both!
    private UUID billedToDepartmentId;  // Pass this OR Institution, not both!
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private List<LineItemRequest> lineItems;

    @Data
    public static class LineItemRequest {
        private ReferenceType referenceType;
        private UUID referenceId; // Booking ID or Maintenance ID
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }
}