package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.ReferenceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {
    private UUID id;
    private String billedToInstitutionName;
    private String billedToDepartmentName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private List<LineItemResponse> lineItems;

    @Data
    @Builder
    public static class LineItemResponse {
        private UUID id;
        private ReferenceType referenceType;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}