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

    // Billing period (for invoice description)
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotalAmount;   // sum of line items before overhead
    private BigDecimal taxAmount;        // computed: subtotal × overheadRate
    private BigDecimal totalAmount;      // grand total including overhead
    private BigDecimal overheadRate;
    private InvoiceStatus status;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String notes;
    private List<LineItemResponse> lineItems;
    private List<TransactionResponse> transactions;

    /**
     * Static vendor/platform identity block for the "Remit To" section.
     * Populated server-side so the frontend never needs to hard-code it.
     */
    private VendorProfile vendor;

    @Data
    @Builder
    public static class VendorProfile {
        private String platformName;       // "LabResource"
        private String legalEntity;        // "SBM TechServices Pvt. Ltd."
        private String addressLine1;       // "Block A, Research Park"
        private String addressLine2;       // "New Delhi – 110001, India"
        private String email;              // "billing@labresource.edu"
        private String phone;              // "+91-11-2345-6789"
        private String taxId;             // "GSTIN: 07AABCS1234Q1ZX"
        private String website;            // "www.labresource.edu"
    }


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