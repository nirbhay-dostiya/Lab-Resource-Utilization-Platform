package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.WaitlistStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WaitlistDto {
    private UUID id;
    private UUID equipmentId;
    private String equipmentName;
    private UUID userId;
    private String userName;
    private LocalDateTime requestedStart;
    private LocalDateTime requestedEnd;
    private WaitlistStatus status;
    private Integer position;
}
