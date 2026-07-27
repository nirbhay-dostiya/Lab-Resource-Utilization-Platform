package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.service;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.IotTelemetryRequestDto;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.IotTelemetryLog;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository.IotTelemetryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IotTelemetryService {

    private final IotTelemetryLogRepository iotTelemetryLogRepository;
    private final EquipmentRepository equipmentRepository;

    @Transactional
    public void ingestTelemetry(IotTelemetryRequestDto requestDto) {
        Equipment equipment = equipmentRepository.findById(requestDto.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("Equipment not found"));

        IotTelemetryLog log = IotTelemetryLog.builder()
                .equipment(equipment)
                .recordedAt(requestDto.getRecordedAt())
                .sensorStatus(requestDto.getSensorStatus())
                .readingValue(requestDto.getReadingValue())
                .build();

        iotTelemetryLogRepository.save(log);
    }
}
