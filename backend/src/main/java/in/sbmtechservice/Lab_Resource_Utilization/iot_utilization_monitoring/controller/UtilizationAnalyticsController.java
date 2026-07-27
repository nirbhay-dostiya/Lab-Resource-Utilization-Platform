package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.controller;

import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.DailyUtilizationMetric;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository.DailyUtilizationMetricRepository;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.service.UtilizationAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utilization-analytics")
@RequiredArgsConstructor
public class UtilizationAnalyticsController {

    private final UtilizationAnalyticsService utilizationAnalyticsService;
    private final DailyUtilizationMetricRepository metricRepository;

    @GetMapping("/heatmap")
    public ResponseEntity<List<DailyUtilizationMetric>> getHeatmapData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DailyUtilizationMetric> data = metricRepository.findAll();
        // In a real application we would filter by date, e.g.:
        // List<DailyUtilizationMetric> data = metricRepository.findByRecordDateBetween(startDate, endDate);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<DailyUtilizationMetric>> getEquipmentMetrics(
            @PathVariable UUID equipmentId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DailyUtilizationMetric> data = metricRepository
                .findByEquipmentIdAndRecordDateBetweenOrderByRecordDateAsc(equipmentId, startDate, endDate);
        return ResponseEntity.ok(data);
    }
}
