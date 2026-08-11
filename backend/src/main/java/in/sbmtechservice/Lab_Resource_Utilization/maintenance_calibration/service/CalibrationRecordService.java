package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.CalibrationRecordRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.CalibrationRecordResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.CalibrationRecord;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository.CalibrationRecordRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalibrationRecordService {

    private final CalibrationRecordRepository calibrationRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CalibrationRecordResponse logCalibration(CalibrationRecordRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

        User calibratedBy = null;
        if (request.getCalibratedById() != null) {
            calibratedBy = userRepository.findById(request.getCalibratedById())
                    .orElseThrow(() -> new IllegalArgumentException("Internal Technician not found."));
        }

        CalibrationRecord record = CalibrationRecord.builder()
                .equipment(equipment)
                .calibratedBy(calibratedBy)
                .vendorName(request.getVendorName())
                .calibrationDate(request.getCalibrationDate())
                .expiryDate(request.getExpiryDate())
                .certificateUrl(request.getCertificateUrl())
                .status(request.getStatus())
                .toleranceMetrics(request.getToleranceMetrics())
                .build();

        CalibrationRecord saved = calibrationRepository.save(record);

        // Notify Lab Managers + Dept Heads of the equipment's institution
        // Actor (loggedBy) gets a self-confirmation
        UUID institutionId = null;
        if (equipment.getDepartment() != null && equipment.getDepartment().getInstitution() != null) {
            institutionId = equipment.getDepartment().getInstitution().getId();
        }

        UUID loggedById = calibratedBy != null ? calibratedBy.getId() : null;
        String loggedByName = calibratedBy != null
                ? calibratedBy.getFirstName() + " " + calibratedBy.getLastName()
                : (request.getVendorName() != null ? request.getVendorName() : "System");
        String expiryDateStr = request.getExpiryDate() != null ? request.getExpiryDate().toString() : "N/A";

        if (institutionId != null) {
            eventPublisher.publishEvent(new NotificationEvents.CalibrationLoggedEvent(
                    institutionId, saved.getId(), equipment.getId(),
                    equipment.getName(), expiryDateStr, loggedById, loggedByName
            ));
        }

        return mapToResponse(saved);
    }

    public List<CalibrationRecordResponse> getRecordsByEquipment(UUID equipmentId) {
        return calibrationRepository.findByEquipmentIdOrderByCalibrationDateDesc(equipmentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CalibrationRecordResponse> getAllRecords() {
        return calibrationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Compliance Dashboard query — returns one record per equipment (the latest),
     * with a computed complianceStatus badge for the frontend.
     *
     * Badge logic:
     *  - EXPIRED:        expiryDate < today
     *  - EXPIRING_SOON:  expiryDate <= today + 30 days
     *  - COMPLIANT:      expiryDate > today + 30 days
     *  - NON_COMPLIANT:  calibration status == FAIL (regardless of date)
     */
    public List<CalibrationRecordResponse> getComplianceDashboard() {
        LocalDate today = LocalDate.now();
        return calibrationRepository.findLatestPerEquipment().stream()
                .map(record -> {
                    CalibrationRecordResponse resp = mapToResponse(record);
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, record.getExpiryDate());

                    if ("FAIL".equalsIgnoreCase(record.getStatus().name())) {
                        resp.setComplianceStatus("NON_COMPLIANT");
                    } else if (daysUntilExpiry < 0) {
                        resp.setComplianceStatus("EXPIRED");
                    } else if (daysUntilExpiry <= 30) {
                        resp.setComplianceStatus("EXPIRING_SOON");
                    } else {
                        resp.setComplianceStatus("COMPLIANT");
                    }
                    resp.setDaysUntilExpiry(daysUntilExpiry);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    public CalibrationRecordResponse mapToResponse(CalibrationRecord record) {
        String techName = record.getCalibratedBy() != null
                ? record.getCalibratedBy().getFirstName() + " " + record.getCalibratedBy().getLastName()
                : record.getVendorName();

        return CalibrationRecordResponse.builder()
                .id(record.getId())
                .equipmentId(record.getEquipment().getId())
                .equipmentName(record.getEquipment().getName())
                .performedBy(techName)
                .vendorName(record.getVendorName())
                .calibrationDate(record.getCalibrationDate())
                .nextDueDate(record.getExpiryDate())
                .certificateUrl(record.getCertificateUrl())
                .result(record.getStatus().name())
                .toleranceMetrics(record.getToleranceMetrics())
                .build();
    }
}