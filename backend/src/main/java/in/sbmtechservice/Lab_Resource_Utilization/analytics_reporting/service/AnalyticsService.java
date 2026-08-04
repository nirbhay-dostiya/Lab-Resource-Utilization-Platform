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

    public org.springframework.data.domain.Page<BookingResponse> getBookingDetails(String scope, UUID scopeId, BookingStatus status, Pageable pageable) {
        org.springframework.data.domain.Page<in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking> bookingPage;

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

    // ── Persona Dashboards (Module 4) ─────────────────────────────────────────

    /**
     * RESEARCHER persona: personal booking history, upcoming reservations, monthly spend.
     */
    public in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.ResearcherDashboardDto getResearcherDashboard(UUID userId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Bookings this month
        long bookingsThisMonth = bookingRepository.findByUserId(userId).stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(monthStart))
                .count();

        // Upcoming bookings (next 30 days)
        java.time.LocalDateTime thirtyDaysLater = now.plusDays(30);
        var upcoming = bookingRepository.findUpcomingByUserId(userId, now).stream()
                .filter(b -> b.getStartTime().isBefore(thirtyDaysLater))
                .limit(10)
                .map(b -> in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.ResearcherDashboardDto.UpcomingBooking.builder()
                        .bookingId(b.getId())
                        .equipmentName(b.getEquipment().getName())
                        .startTime(b.getStartTime())
                        .endTime(b.getEndTime())
                        .status(b.getStatus().name())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // Monthly spend — best-effort from booking prices
        java.math.BigDecimal spendThisMonth = bookingRepository.findByUserId(userId).stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(monthStart))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> {
                    long hours = java.time.Duration.between(b.getStartTime(), b.getEndTime()).toHours();
                    return b.getEquipment().getPricePerHour()
                            .multiply(java.math.BigDecimal.valueOf(Math.max(hours, 1)));
                })
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.ResearcherDashboardDto.builder()
                .bookingsThisMonth(bookingsThisMonth)
                .spendThisMonth(spendThisMonth)
                .upcomingBookings(upcoming)
                .build();
    }

    /**
     * LAB_MANAGER / DEPT_HEAD persona: equipment utilization, work orders, institution summary.
     */
    public in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.LabManagerDashboardDto getLabManagerDashboard(UUID institutionId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime thirtyDaysAgo = now.minusDays(30);

        long totalEquipment = equipmentRepository.countByDepartmentInstitutionId(institutionId);
        long underMaintenance = equipmentRepository.countByDepartmentInstitutionIdAndStatus(institutionId, EquipmentStatus.UNDER_MAINTENANCE);
        long bookingsThisMonth = bookingRepository.countByEquipmentDepartmentInstitutionId(institutionId);

        // Work orders by status
        var allStatuses = java.util.Arrays.stream(
                in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.enums.MaintenanceStatus.values())
                .collect(java.util.stream.Collectors.toList());

        // Utilization rates per equipment
        List<Object[]> utilizationData = bookingRepository.findEquipmentUtilizationHours(institutionId, thirtyDaysAgo);
        double availableHoursInPeriod = 30 * 24.0; // 30 days

        var utilizationRates = utilizationData.stream()
                .map(row -> {
                    double usageHours = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    return in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.LabManagerDashboardDto.EquipmentUtilization.builder()
                            .equipmentId(row[0].toString())
                            .equipmentName((String) row[1])
                            .usageHours(usageHours)
                            .utilizationPct(Math.min(100.0, usageHours / availableHoursInPeriod * 100.0))
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());

        Map<String, Double> topEquipment = utilizationRates.stream()
                .sorted((a, b) -> Double.compare(b.getUsageHours(), a.getUsageHours()))
                .limit(5)
                .collect(java.util.stream.Collectors.toMap(
                        in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.LabManagerDashboardDto.EquipmentUtilization::getEquipmentName,
                        in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.LabManagerDashboardDto.EquipmentUtilization::getUsageHours,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));

        return in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.LabManagerDashboardDto.builder()
                .totalEquipment(totalEquipment)
                .underMaintenance(underMaintenance)
                .workOrdersByStatus(java.util.Collections.emptyMap()) // populated via separate maintenance repo
                .topEquipmentByUsageHours(topEquipment)
                .utilizationRates(utilizationRates)
                .bookingsThisMonth(bookingsThisMonth)
                .build();
    }

    /**
     * SYSTEM_ADMIN persona: cross-institution financial health and system bottlenecks.
     */
    public in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.SystemAdminDashboardDto getSystemAdminDashboard() {
        long totalEquipment = equipmentRepository.count();
        long underMaintenance = equipmentRepository.countByStatus(EquipmentStatus.UNDER_MAINTENANCE);
        long totalBookings = bookingRepository.count();
        long bookingsToday = bookingRepository.findAll().stream()
                .filter(b -> b.getStartTime() != null &&
                        b.getStartTime().toLocalDate().equals(java.time.LocalDate.now()))
                .count();

        // Top booked equipment
        List<Object[]> topData = bookingRepository.countBookingsByEquipmentGlobal();
        Map<String, Long> topBooked = topData.stream()
                .limit(5)
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));

        return in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.dto.SystemAdminDashboardDto.builder()
                .totalEquipment(totalEquipment)
                .underMaintenanceCount(underMaintenance)
                .totalInstitutions(0) // will be filled by controller from InstitutionRepository
                .totalUsers(0)
                .totalInvoiced(java.math.BigDecimal.ZERO)
                .totalPaid(java.math.BigDecimal.ZERO)
                .totalOverdue(java.math.BigDecimal.ZERO)
                .openWorkOrders(0)
                .topBookedEquipment(topBooked)
                .bookingsToday(bookingsToday)
                .pendingApprovalByInstitution(java.util.Collections.emptyList())
                .build();
    }
}

