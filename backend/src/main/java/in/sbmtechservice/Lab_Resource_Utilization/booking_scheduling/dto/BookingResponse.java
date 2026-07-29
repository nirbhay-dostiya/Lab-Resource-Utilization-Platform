package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private UUID equipmentId;
    private String equipmentName;
    private UUID equipmentInstitutionId;
    private String equipmentInstitutionName;
    private String userInstitutionName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private BookingStatus status;
    private UUID invoiceId;
    private java.math.BigDecimal totalAmount;
}