package in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.service;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.DailyUtilizationMetric;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.entity.IotTelemetryLog;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.enums.SensorStatus;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository.DailyUtilizationMetricRepository;
import in.sbmtechservice.Lab_Resource_Utilization.iot_utilization_monitoring.repository.IotTelemetryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilizationAnalyticsService {

    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final IotTelemetryLogRepository telemetryLogRepository;
    private final DailyUtilizationMetricRepository metricRepository;

    @Scheduled(cron = "0 0 1 * * ?") // Run every day at 1 AM
    @Transactional
    public void calculateDailyMetricsJob() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Equipment> allEquipment = equipmentRepository.findAll();
        
        for (Equipment equipment : allEquipment) {
            calculateAndSaveDailyMetric(equipment, yesterday);
        }
    }

    @jakarta.annotation.PostConstruct
    @Transactional
    public void seedInitialMetrics() {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        if (allEquipment.isEmpty()) {
            log.warn("seedInitialMetrics: No equipment found in database — skipping seed. Add equipment first.");
            return;
        }
        log.info("seedInitialMetrics: Backfilling last 7 days of utilization metrics for {} equipment items.", allEquipment.size());
        int seeded = 0;
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            for (Equipment equipment : allEquipment) {
                // Idempotent: only calculate if no record exists yet for this equipment+date
                boolean alreadyExists = metricRepository
                        .findByEquipmentIdAndRecordDate(equipment.getId(), date)
                        .isPresent();
                if (!alreadyExists) {
                    calculateAndSaveDailyMetric(equipment, date);
                    seeded++;
                }
            }
        }
        log.info("seedInitialMetrics: Seeded {} new metric records.", seeded);
    }

    /**
     * Manual recalculation trigger — called by the admin /recalculate endpoint.
     * Recalculates (upserts) metrics for the last {@code daysBack} days across all equipment.
     */
    @Transactional
    public void recalculateRange(int daysBack) {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        log.info("recalculateRange: Recalculating last {} days for {} equipment items.", daysBack, allEquipment.size());
        for (int i = daysBack - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            for (Equipment equipment : allEquipment) {
                calculateAndSaveDailyMetric(equipment, date);
            }
        }
        log.info("recalculateRange: Done.");
    }

    public void calculateAndSaveDailyMetric(Equipment equipment, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Calculate booked minutes
        List<Booking> bookings = bookingRepository.findByEquipmentId(equipment.getId());
        int totalBookedMinutes = 0;
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED) {
                LocalDateTime overlapStart = booking.getStartTime().isBefore(startOfDay) ? startOfDay : booking.getStartTime();
                LocalDateTime overlapEnd = booking.getEndTime().isAfter(endOfDay) ? endOfDay : booking.getEndTime();
                
                if (overlapStart.isBefore(overlapEnd)) {
                    totalBookedMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
                }
            }
        }

        // Calculate used minutes from telemetry
        // Simplified assumption: if sensor was ACTIVE, we count the duration between logs, 
        // up to max 10 mins per log gap.
        List<IotTelemetryLog> logs = telemetryLogRepository
                .findByEquipmentIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                        equipment.getId(), startOfDay, endOfDay, PageRequest.of(0, 1000))
                .getContent();

        int totalUsedMinutes = 0;
        if (!logs.isEmpty()) {
            // Sort by time ascending for easier calculation
            logs.sort((a, b) -> a.getRecordedAt().compareTo(b.getRecordedAt()));
            for (int i = 0; i < logs.size() - 1; i++) {
                IotTelemetryLog current = logs.get(i);
                IotTelemetryLog next = logs.get(i + 1);
                
                if (current.getSensorStatus() == SensorStatus.ACTIVE) {
                    long minutes = Duration.between(current.getRecordedAt(), next.getRecordedAt()).toMinutes();
                    totalUsedMinutes += Math.min(minutes, 30); // Cap gap at 30 mins
                }
            }
            // Add a fixed duration for the last log if active
            if (logs.get(logs.size() - 1).getSensorStatus() == SensorStatus.ACTIVE) {
                 totalUsedMinutes += 10;
            }
        }

        BigDecimal utilizationRate = BigDecimal.ZERO;
        if (totalBookedMinutes > 0) {
            utilizationRate = BigDecimal.valueOf((double) totalUsedMinutes / totalBookedMinutes * 100)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        int idleTimeMinutes = Math.max(0, totalBookedMinutes - totalUsedMinutes);

        DailyUtilizationMetric metric = metricRepository
                .findByEquipmentIdAndRecordDate(equipment.getId(), date)
                .orElse(new DailyUtilizationMetric());

        metric.setEquipment(equipment);
        metric.setRecordDate(date);
        metric.setTotalBookedMinutes(totalBookedMinutes);
        metric.setTotalUsedMinutes(totalUsedMinutes);
        metric.setUtilizationRate(utilizationRate);
        metric.setIdleTimeMinutes(idleTimeMinutes);
        
        metricRepository.save(metric);
    }
    
    public List<DailyUtilizationMetric> getUtilizationHeatmapData(LocalDate startDate, LocalDate endDate) {
        // Here we could fetch all metrics between dates to display a heatmap
        // Since we don't have a specific endpoint, we'll return all for the period
        return metricRepository.findAll(); // simplified for now, would use custom query
    }
}
