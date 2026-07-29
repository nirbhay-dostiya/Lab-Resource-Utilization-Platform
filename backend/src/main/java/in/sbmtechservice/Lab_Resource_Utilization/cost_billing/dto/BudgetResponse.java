package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {
    private UUID id;
    private UUID departmentId;
    private String departmentName;
    private Integer fiscalYear;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
}
