package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.scheduler;

import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.CalibrationRecord;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository.CalibrationRecordRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily background worker that fires calibration reminder notifications.
 *
 * Runs at 07:00 every day (cron: 0 0 7 * * *).
 * Queries for calibration records expiring in exactly 30, 14, 7, and 1 day(s).
 * Publishes a CalibrationReminderEvent for each match → picked up by NotificationEventListener
 * which routes to in-app SSE push AND email (Module 6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalibrationReminderScheduler {

    private final CalibrationRecordRepository calibrationRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Reminder thresholds in days — industry standard calibration escalation ladder. */
    private static final int[] REMINDER_DAYS = {30, 14, 7, 1};

    @Scheduled(cron = "0 0 7 * * *") // Daily at 07:00 server time
    public void checkCalibrationExpirations() {
        log.info("[CALIBRATION-SCHEDULER] Running daily calibration expiry check...");
        LocalDate today = LocalDate.now();
        int totalFired = 0;

        for (int days : REMINDER_DAYS) {
            LocalDate targetDate = today.plusDays(days);
            List<CalibrationRecord> expiringRecords = calibrationRecordRepository.findByExpiryDate(targetDate);

            for (CalibrationRecord record : expiringRecords) {
                UUID institutionId = null;
                try {
                    institutionId = record.getEquipment().getDepartment().getInstitution().getId();
                } catch (Exception e) {
                    log.warn("[CALIBRATION-SCHEDULER] Could not resolve institution for record {}", record.getId());
                }

                eventPublisher.publishEvent(new NotificationEvents.CalibrationReminderEvent(
                        institutionId,
                        record.getId(),
                        record.getEquipment().getId(),
                        record.getEquipment().getName(),
                        record.getExpiryDate().toString(),
                        days
                ));
                totalFired++;
                log.debug("[CALIBRATION-SCHEDULER] Fired {}d reminder for '{}' (expires {})",
                        days, record.getEquipment().getName(), record.getExpiryDate());
            }
        }

        log.info("[CALIBRATION-SCHEDULER] Completed — fired {} reminder events.", totalFired);
    }
}
