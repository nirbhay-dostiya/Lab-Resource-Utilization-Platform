package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.controller;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.dto.WaitlistDto;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.WaitlistStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/waitlists")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/join")
    public ResponseEntity<WaitlistDto> joinWaitlist(@RequestBody WaitlistDto dto) {
        return ResponseEntity.ok(waitlistService.joinWaitlist(dto));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WaitlistDto>> getUserWaitlists(@PathVariable UUID userId) {
        return ResponseEntity.ok(waitlistService.getUserWaitlists(userId));
    }

    @PutMapping("/{waitlistId}/status")
    public ResponseEntity<WaitlistDto> updateWaitlistStatus(@PathVariable UUID waitlistId, @RequestParam WaitlistStatus status) {
        return ResponseEntity.ok(waitlistService.updateWaitlistStatus(waitlistId, status));
    }
}
