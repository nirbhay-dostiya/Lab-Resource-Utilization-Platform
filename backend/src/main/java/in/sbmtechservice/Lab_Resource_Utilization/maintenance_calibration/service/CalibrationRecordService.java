package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.CalibrationRecordRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.CalibrationRecordResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.CalibrationRecord;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository.CalibrationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalibrationRecordService {

    private final CalibrationRecordRepository calibrationRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

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
                .build();

        CalibrationRecord saved = calibrationRepository.save(record);
        return mapToResponse(saved);
    }

    public java.util.List<CalibrationRecordResponse> getRecordsByEquipment(UUID equipmentId) {
        return calibrationRepository.findByEquipmentIdOrderByCalibrationDateDesc(equipmentId).stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<CalibrationRecordResponse> getAllRecords() {
        return calibrationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private CalibrationRecordResponse mapToResponse(CalibrationRecord record) {
        String techName = record.getCalibratedBy() != null ?
                record.getCalibratedBy().getFirstName() + " " + record.getCalibratedBy().getLastName() : record.getVendorName();

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
                .build();
    }
}