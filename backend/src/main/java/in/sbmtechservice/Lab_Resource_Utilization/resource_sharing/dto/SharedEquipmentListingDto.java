package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SharedEquipmentListingDto {
    private UUID id;
    private UUID agreementId;
    private UUID equipmentId;
    private String equipmentName;
    private BigDecimal externalHourlyRate;
    private String termsAndConditions;
    private String institutionName;
    private UUID institutionId;
    private java.time.LocalDate availableFrom;
    private java.time.LocalDate availableTo;
    private Boolean isActive;
    private Integer waitlistCount;
}
