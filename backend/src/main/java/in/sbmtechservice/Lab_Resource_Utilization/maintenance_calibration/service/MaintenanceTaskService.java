package in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskRequest;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.dto.MaintenanceTaskResponse;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.entity.MaintenanceTask;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository.MaintenanceTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceTaskService {

    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public MaintenanceTaskResponse scheduleTask(MaintenanceTaskRequest request) {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

        User technician = null;
        if (request.getTechnicianId() != null) {
            technician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new IllegalArgumentException("Technician not found."));
        }

        MaintenanceTask task = MaintenanceTask.builder()
                .equipment(equipment)
                .technician(technician)
                .maintenanceType(request.getMaintenanceType())
                .scheduledDate(request.getScheduledDate())
                .description(request.getDescription())
                .cost(request.getEstimatedCost() != null ? request.getEstimatedCost() : BigDecimal.ZERO)
                .status(MaintenanceStatus.valueOf("SCHEDULED")) // Ensure "SCHEDULED" is in your Enum!
                .build();

        // Lock equipment
        equipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
        equipmentRepository.save(equipment);

        MaintenanceTask saved = maintenanceTaskRepository.save(task);
        return mapToResponse(saved);
    }

    @Transactional
    public MaintenanceTaskResponse completeTask(UUID taskId, String resolutionNotes, BigDecimal finalCost) {
        MaintenanceTask task = maintenanceTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));

        task.setStatus(MaintenanceStatus.valueOf("COMPLETED")); // Ensure "COMPLETED" is in your Enum!
        task.setCompletedDate(LocalDateTime.now());
        task.setResolutionNotes(resolutionNotes);
        if (finalCost != null) {
            task.setCost(finalCost);
        }

        // Unlock equipment
        Equipment equipment = task.getEquipment();
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipmentRepository.save(equipment);

        MaintenanceTask saved = maintenanceTaskRepository.save(task);
        return mapToResponse(saved);
    }

    private MaintenanceTaskResponse mapToResponse(MaintenanceTask task) {
        String techName = task.getTechnician() != null ?
                task.getTechnician().getFirstName() + " " + task.getTechnician().getLastName() : "Unassigned";

        return MaintenanceTaskResponse.builder()
                .id(task.getId())
                .equipmentId(task.getEquipment().getId())
                .equipmentName(task.getEquipment().getName())
                .technicianName(techName)
                .maintenanceType(task.getMaintenanceType())
                .status(task.getStatus())
                .scheduledDate(task.getScheduledDate())
                .completedDate(task.getCompletedDate())
                .description(task.getDescription())
                .resolutionNotes(task.getResolutionNotes())
                .cost(task.getCost())
                .build();
    }
}