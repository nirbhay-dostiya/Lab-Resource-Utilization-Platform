package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.ReferenceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
public class InvoiceResponse {
    private UUID id;
    private String billedToInstitutionName;
    private String billedToDepartmentName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal overheadRate;
    private InvoiceStatus status;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String notes;
    private List<LineItemResponse> lineItems;
    private List<TransactionResponse> transactions;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemResponse {
        private UUID id;
        private ReferenceType referenceType;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String equipmentName;
        private String equipmentInstituteName;
        private LocalDateTime bookingStartTime;
        private LocalDateTime bookingEndTime;
        private UUID referenceId;
    }
}