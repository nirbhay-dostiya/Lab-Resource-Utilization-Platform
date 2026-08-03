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
import org.springframework.data.domain.PageRequest;
import java.util.Map;
import java.util.HashMap;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.DailyAggregatedUtilization;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.dto.EquipmentUtilizationStat;

@RestController
@RequestMapping("/api/v1/utilization-analytics")
@RequiredArgsConstructor
public class UtilizationAnalyticsController {

    private final UtilizationAnalyticsService utilizationAnalyticsService;
    private final DailyUtilizationMetricRepository metricRepository;

    @GetMapping("/heatmap")
    public ResponseEntity<List<DailyAggregatedUtilization>> getHeatmapData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DailyAggregatedUtilization> data = metricRepository.getAggregatedMetrics(startDate, endDate);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<String, List<EquipmentUtilizationStat>>> getPerformanceStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<EquipmentUtilizationStat> top = metricRepository.getTopUtilizedEquipment(startDate, endDate, PageRequest.of(0, 5));
        List<EquipmentUtilizationStat> bottom = metricRepository.getLeastUtilizedEquipment(startDate, endDate, PageRequest.of(0, 5));
        
        Map<String, List<EquipmentUtilizationStat>> response = new HashMap<>();
        response.put("top", top);
        response.put("bottom", bottom);
        return ResponseEntity.ok(response);
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
