package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto;

import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.enums.SensorStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class IotTelemetryRequestDto {
    private UUID equipmentId;
    private LocalDateTime recordedAt;
    private SensorStatus sensorStatus;
    private BigDecimal readingValue;
}
