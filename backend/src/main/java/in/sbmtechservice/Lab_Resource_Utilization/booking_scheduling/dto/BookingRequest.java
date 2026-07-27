package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingRequest {
    private UUID equipmentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
}