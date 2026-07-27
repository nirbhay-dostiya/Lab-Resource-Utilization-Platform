package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.controller;

import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.IotTelemetryRequestDto;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.service.IotTelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iot-telemetry")
@RequiredArgsConstructor
public class IotTelemetryController {

    private final IotTelemetryService iotTelemetryService;

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingestTelemetry(@RequestBody IotTelemetryRequestDto requestDto) {
        iotTelemetryService.ingestTelemetry(requestDto);
        return ResponseEntity.ok().build();
    }
}
