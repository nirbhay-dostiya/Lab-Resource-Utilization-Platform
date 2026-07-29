package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.service;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.AnalyticsResponse;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;

    public AnalyticsResponse getInstitutionAnalytics(UUID institutionId) {
        List<Equipment> equipmentList = equipmentRepository.findByDepartmentInstitutionId(institutionId);
        List<Booking> bookings = bookingRepository.findByEquipmentDepartmentInstitutionId(institutionId);

        return buildResponse(equipmentList, bookings);
    }

    public AnalyticsResponse getDepartmentAnalytics(UUID departmentId) {
        List<Equipment> equipmentList = equipmentRepository.findByDepartmentId(departmentId);
        List<Booking> bookings = bookingRepository.findByEquipmentDepartmentId(departmentId);

        return buildResponse(equipmentList, bookings);
    }

    public AnalyticsResponse getGlobalAnalytics() {
        List<Equipment> equipmentList = equipmentRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        return buildResponse(equipmentList, bookings);
    }

    private AnalyticsResponse buildResponse(List<Equipment> equipmentList, List<Booking> bookings) {
        long totalEquipment = equipmentList.size();
        long underMaintenance = equipmentList.stream()
                .filter(e -> e.getStatus() == EquipmentStatus.UNDER_MAINTENANCE)
                .count();

        long totalBookings = bookings.size();
        long pendingApprovals = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();

        Map<String, Long> bookingsByEquipment = bookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getEquipment().getName(),
                        Collectors.counting()
                ));

        return AnalyticsResponse.builder()
                .totalEquipment(totalEquipment)
                .underMaintenance(underMaintenance)
                .totalBookings(totalBookings)
                .pendingApprovals(pendingApprovals)
                .bookingsByEquipment(bookingsByEquipment)
                .build();
    }
}
