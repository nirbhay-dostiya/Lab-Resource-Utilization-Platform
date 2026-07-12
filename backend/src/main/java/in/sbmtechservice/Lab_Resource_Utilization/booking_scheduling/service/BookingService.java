package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.BookingRequest;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.BookingResponse;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        // 1. Time Validation
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book equipment in the past.");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after the start time.");
        }

        // 2. Fetch User & Equipment
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

        // 3. Equipment Status Check (🚨 UPDATED TO MATCH YOUR ENUM 🚨)
        if (equipment.getStatus() == EquipmentStatus.OUT_OF_SERVICE ||
                equipment.getStatus() == EquipmentStatus.UNDER_MAINTENANCE ||
                equipment.getStatus() == EquipmentStatus.RETIRED) {
            throw new IllegalStateException("This equipment is currently out of service, under maintenance, or retired and cannot be booked.");
        }

        // 4. Overlap Security Check
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                equipment.getId(), request.getStartTime(), request.getEndTime());

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalStateException("This time slot is already booked for this equipment.");
        }

        // 5. Save Booking
        Booking booking = Booking.builder()
                .user(user)
                .equipment(equipment)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .purpose(request.getPurpose())
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking saved = bookingRepository.save(booking);
        return mapToResponse(saved);
    }

    @Transactional
    public BookingResponse updateBookingStatus(UUID bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        return mapToResponse(saved);
    }

    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        
        boolean isSystemAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.SYSTEM_ADMIN);
        boolean isInstAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.INSTITUTION_ADMIN);
        boolean isDeptHead = user.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.DEPT_HEAD);

        List<Booking> bookings;

        if (isSystemAdmin) {
            bookings = bookingRepository.findAll();
        } else if (isInstAdmin) {
            UUID instId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (instId == null) throw new SecurityException("Institution Admin is not associated with an institution.");
            bookings = bookingRepository.findByEquipmentDepartmentInstitutionId(instId);
        } else if (isDeptHead) {
            if (user.getDepartment() == null) throw new SecurityException("Department Head is not associated with a department.");
            bookings = bookingRepository.findByEquipmentDepartmentId(user.getDepartment().getId());
        } else {
            throw new SecurityException("You do not have permission to view all bookings.");
        }

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .equipmentId(booking.getEquipment().getId())
                .equipmentName(booking.getEquipment().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .purpose(booking.getPurpose())
                .status(booking.getStatus())
                .build();
    }
}