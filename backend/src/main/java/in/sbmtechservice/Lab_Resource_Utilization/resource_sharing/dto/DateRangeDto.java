package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeDto {
    private LocalDate start;
    private LocalDate end;
    private String reason; // Optional: e.g., "Booked", "Approved Access Request"
}
