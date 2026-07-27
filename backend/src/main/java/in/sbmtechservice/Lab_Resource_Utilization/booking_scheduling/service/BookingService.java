package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.BookingRequest;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.BookingResponse;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.WaitlistRepository;
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
    private final WaitlistRepository waitlistRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        // 1. Time Validation
        if (request.getStartTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
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

        // 4.5. Cross-Institute Booking Validation for Lab Managers
        boolean isLabManager = user.getRoles().stream()
                .anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.LAB_MANAGER);

        if (isLabManager) {
            UUID userInstId = user.getInstitution() != null ? user.getInstitution().getId() 
                    : (user.getDepartment() != null && user.getDepartment().getInstitution() != null 
                        ? user.getDepartment().getInstitution().getId() : null);
            UUID eqInstId = equipment.getDepartment().getInstitution().getId();
            
            if (userInstId != null && userInstId.equals(eqInstId)) {
                throw new SecurityException("Lab Managers can only book equipment belonging to other institutes, not their own.");
            }
        }

        // 5. Save Booking
        Booking booking = Booking.builder()
                .user(user)
                .equipment(equipment)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .purpose(request.getPurpose())
                .status(BookingStatus.PENDING)
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

        if (newStatus == BookingStatus.CANCELLED) {
            // Auto-allocate waitlist
            List<in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Waitlist> waitlists = waitlistRepository
                    .findByEquipmentIdAndStatusOrderByCreatedAtAsc(booking.getEquipment().getId(), in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.WaitlistStatus.ACTIVE);
            
            for (in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Waitlist waitlist : waitlists) {
                // Simplified overlap check: check if waitlist timeslot falls within the cancelled booking's slot
                if (!waitlist.getRequestedStart().isBefore(booking.getStartTime()) && 
                    !waitlist.getRequestedEnd().isAfter(booking.getEndTime())) {
                    
                    // Create new booking
                    Booking newBooking = Booking.builder()
                            .user(waitlist.getUser())
                            .equipment(booking.getEquipment())
                            .startTime(waitlist.getRequestedStart())
                            .endTime(waitlist.getRequestedEnd())
                            .purpose("Auto-allocated from waitlist (Pending Approval)")
                            .status(BookingStatus.PENDING)
                            .build();
                    bookingRepository.save(newBooking);

                    // Update waitlist status
                    waitlist.setStatus(in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.WaitlistStatus.FULFILLED);
                    waitlistRepository.save(waitlist);
                    break; // Only allocate one waitlist per cancellation for simplicity
                }
            }
        }

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
        boolean isInstAdminOrLabManager = user.getRoles().stream().anyMatch(r -> 
            r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.INSTITUTION_ADMIN || 
            r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.LAB_MANAGER);
        boolean isDeptHead = user.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.DEPT_HEAD);

        List<Booking> bookings;

        if (isSystemAdmin) {
            bookings = bookingRepository.findAll();
        } else if (isInstAdminOrLabManager) {
            UUID instId = user.getInstitution() != null ? user.getInstitution().getId() : (user.getDepartment() != null && user.getDepartment().getInstitution() != null ? user.getDepartment().getInstitution().getId() : null);
            if (instId == null) {
                bookings = new java.util.ArrayList<>(bookingRepository.findByUserId(user.getId()));
            } else {
                List<Booking> instBookings = bookingRepository.findByEquipmentDepartmentInstitutionId(instId);
                List<Booking> myBookings = bookingRepository.findByUserId(user.getId());
                java.util.Map<UUID, Booking> bookingMap = new java.util.LinkedHashMap<>();
                instBookings.forEach(b -> bookingMap.put(b.getId(), b));
                myBookings.forEach(b -> bookingMap.put(b.getId(), b));
                bookings = new java.util.ArrayList<>(bookingMap.values());
            }
        } else if (isDeptHead) {
            if (user.getDepartment() == null) {
                bookings = new java.util.ArrayList<>(bookingRepository.findByUserId(user.getId()));
            } else {
                List<Booking> deptBookings = bookingRepository.findByEquipmentDepartmentId(user.getDepartment().getId());
                List<Booking> myBookings = bookingRepository.findByUserId(user.getId());
                java.util.Map<UUID, Booking> bookingMap = new java.util.LinkedHashMap<>();
                deptBookings.forEach(b -> bookingMap.put(b.getId(), b));
                myBookings.forEach(b -> bookingMap.put(b.getId(), b));
                bookings = new java.util.ArrayList<>(bookingMap.values());
            }
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
                .equipmentInstitutionId(booking.getEquipment().getDepartment().getInstitution().getId())
                .equipmentInstitutionName(booking.getEquipment().getDepartment().getInstitution().getName())
                .userInstitutionName(booking.getUser().getInstitution() != null ? booking.getUser().getInstitution().getName() : (booking.getUser().getDepartment() != null ? booking.getUser().getDepartment().getInstitution().getName() : "Unknown"))
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .purpose(booking.getPurpose())
                .status(booking.getStatus())
                .build();
    }
}