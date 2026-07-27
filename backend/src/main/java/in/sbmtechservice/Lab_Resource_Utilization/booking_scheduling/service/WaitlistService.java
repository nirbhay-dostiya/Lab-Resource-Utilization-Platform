package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.WaitlistDto;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Waitlist;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.WaitlistStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.WaitlistRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public WaitlistDto joinWaitlist(WaitlistDto dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Waitlist waitlist = Waitlist.builder()
                .equipment(equipment)
                .user(user)
                .requestedStart(dto.getRequestedStart())
                .requestedEnd(dto.getRequestedEnd())
                .status(WaitlistStatus.ACTIVE)
                .build();

        Waitlist saved = waitlistRepository.save(waitlist);
        dto.setId(saved.getId());
        dto.setStatus(saved.getStatus());
        dto.setEquipmentName(equipment.getName());
        dto.setUserName(user.getFirstName() + " " + user.getLastName());
        return dto;
    }

    public List<WaitlistDto> getUserWaitlists(UUID userId) {
        return waitlistRepository.findByUserId(userId).stream()
                .map(waitlist -> {
                    WaitlistDto dto = mapToDto(waitlist);
                    if (waitlist.getStatus() == WaitlistStatus.ACTIVE) {
                        List<Waitlist> queue = waitlistRepository.findByEquipmentIdAndStatusOrderByCreatedAtAsc(waitlist.getEquipment().getId(), WaitlistStatus.ACTIVE);
                        int position = 1;
                        for (Waitlist w : queue) {
                            if (w.getId().equals(waitlist.getId())) break;
                            position++;
                        }
                        dto.setPosition(position);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public WaitlistDto updateWaitlistStatus(UUID waitlistId, WaitlistStatus status) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new RuntimeException("Waitlist not found"));
        waitlist.setStatus(status);
        Waitlist saved = waitlistRepository.save(waitlist);
        return mapToDto(saved);
    }
    
    public List<Waitlist> getPendingWaitlistsForEquipment(UUID equipmentId) {
        return waitlistRepository.findByEquipmentIdAndStatusOrderByCreatedAtAsc(equipmentId, WaitlistStatus.ACTIVE);
    }

    private WaitlistDto mapToDto(Waitlist waitlist) {
        WaitlistDto dto = new WaitlistDto();
        dto.setId(waitlist.getId());
        dto.setEquipmentId(waitlist.getEquipment().getId());
        dto.setEquipmentName(waitlist.getEquipment().getName());
        dto.setUserId(waitlist.getUser().getId());
        dto.setUserName(waitlist.getUser().getFirstName() + " " + waitlist.getUser().getLastName());
        dto.setRequestedStart(waitlist.getRequestedStart());
        dto.setRequestedEnd(waitlist.getRequestedEnd());
        dto.setStatus(waitlist.getStatus());
        return dto;
    }
}
