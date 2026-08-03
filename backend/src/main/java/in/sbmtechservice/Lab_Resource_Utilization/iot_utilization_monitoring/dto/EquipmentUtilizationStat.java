package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUtilizationStat {
    private UUID equipmentId;
    private String equipmentName;
    private Double avgUtilizationRate;
    private Long totalIdleMinutes;
}
