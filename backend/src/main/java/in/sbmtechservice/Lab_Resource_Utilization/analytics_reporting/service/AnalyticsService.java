package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.service;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.AnalyticsResponse;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.service.BookingService;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service.EquipmentService;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.BookingResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.EquipmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingService bookingService;
    private final EquipmentService equipmentService;

    public AnalyticsResponse getInstitutionAnalytics(UUID institutionId) {
        long totalEquipment = equipmentRepository.countByDepartmentInstitutionId(institutionId);
        long underMaintenance = equipmentRepository.countByDepartmentInstitutionIdAndStatus(institutionId, EquipmentStatus.UNDER_MAINTENANCE);
        long totalBookings = bookingRepository.countByEquipmentDepartmentInstitutionId(institutionId);
        long pendingApprovals = bookingRepository.countByEquipmentDepartmentInstitutionIdAndStatus(institutionId, BookingStatus.PENDING);
        List<Object[]> bookingsData = bookingRepository.countBookingsByEquipmentForInstitution(institutionId);

        return buildResponse(totalEquipment, underMaintenance, totalBookings, pendingApprovals, bookingsData);
    }

    public AnalyticsResponse getDepartmentAnalytics(UUID departmentId) {
        long totalEquipment = equipmentRepository.countByDepartmentId(departmentId);
        long underMaintenance = equipmentRepository.countByDepartmentIdAndStatus(departmentId, EquipmentStatus.UNDER_MAINTENANCE);
        long totalBookings = bookingRepository.countByEquipmentDepartmentId(departmentId);
        long pendingApprovals = bookingRepository.countByEquipmentDepartmentIdAndStatus(departmentId, BookingStatus.PENDING);
        List<Object[]> bookingsData = bookingRepository.countBookingsByEquipmentForDepartment(departmentId);

        return buildResponse(totalEquipment, underMaintenance, totalBookings, pendingApprovals, bookingsData);
    }

    public AnalyticsResponse getGlobalAnalytics() {
        long totalEquipment = equipmentRepository.count();
        long underMaintenance = equipmentRepository.countByStatus(EquipmentStatus.UNDER_MAINTENANCE);
        long totalBookings = bookingRepository.count();
        long pendingApprovals = bookingRepository.countByStatus(BookingStatus.PENDING);
        List<Object[]> bookingsData = bookingRepository.countBookingsByEquipmentGlobal();

        return buildResponse(totalEquipment, underMaintenance, totalBookings, pendingApprovals, bookingsData);
    }

    private AnalyticsResponse buildResponse(long totalEquipment, long underMaintenance, long totalBookings, long pendingApprovals, List<Object[]> bookingsData) {
        Map<String, Long> bookingsByEquipment = bookingsData.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        return AnalyticsResponse.builder()
                .totalEquipment(totalEquipment)
                .underMaintenance(underMaintenance)
                .totalBookings(totalBookings)
                .pendingApprovals(pendingApprovals)
                .bookingsByEquipment(bookingsByEquipment)
                .build();
    }

    // --- Paginated Drill-Down Methods ---

    public Page<EquipmentResponse> getEquipmentDetails(String scope, UUID scopeId, EquipmentStatus status, Pageable pageable) {
        Page<in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment> equipmentPage;

        if ("institution".equalsIgnoreCase(scope)) {
            if (status != null) equipmentPage = equipmentRepository.findByDepartmentInstitutionIdAndStatus(scopeId, status, pageable);
            else equipmentPage = equipmentRepository.findByDepartmentInstitutionId(scopeId, pageable);
        } else if ("department".equalsIgnoreCase(scope)) {
            if (status != null) equipmentPage = equipmentRepository.findByDepartmentIdAndStatus(scopeId, status, pageable);
            else equipmentPage = equipmentRepository.findByDepartmentId(scopeId, pageable);
        } else { // global
            if (status != null) equipmentPage = equipmentRepository.findByStatus(status, pageable);
            else equipmentPage = equipmentRepository.findAllEquipment(pageable);
        }

        return equipmentPage.map(equipmentService::mapToResponse);
    }

    public Page<BookingResponse> getBookingDetails(String scope, UUID scopeId, BookingStatus status, Pageable pageable) {
        Page<in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking> bookingPage;

        if ("institution".equalsIgnoreCase(scope)) {
            if (status != null) bookingPage = bookingRepository.findByEquipmentDepartmentInstitutionIdAndStatus(scopeId, status, pageable);
            else bookingPage = bookingRepository.findByEquipmentDepartmentInstitutionId(scopeId, pageable);
        } else if ("department".equalsIgnoreCase(scope)) {
            if (status != null) bookingPage = bookingRepository.findByEquipmentDepartmentIdAndStatus(scopeId, status, pageable);
            else bookingPage = bookingRepository.findByEquipmentDepartmentId(scopeId, pageable);
        } else { // global
            if (status != null) bookingPage = bookingRepository.findByStatus(status, pageable);
            else bookingPage = bookingRepository.findAllBookings(pageable);
        }

        return bookingPage.map(bookingService::mapToResponse);
    }
}
