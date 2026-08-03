package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyAggregatedUtilization {
    private LocalDate recordDate;
    private Double avgUtilizationRate;
    private Long totalIdleMinutes;
    private Long totalBookedMinutes;
    private Long totalUsedMinutes;
}
