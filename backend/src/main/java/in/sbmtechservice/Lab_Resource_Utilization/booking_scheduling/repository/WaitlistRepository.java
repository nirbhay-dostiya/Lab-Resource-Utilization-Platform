package in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository;

import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Waitlist;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, UUID> {

    // Fetch a user's waitlist requests
    List<Waitlist> findByUserId(UUID userId);

    // 🚨 FIFO QUEUE LOGIC: Get active waitlist users for equipment, ordered by who asked first 🚨
    List<Waitlist> findByEquipmentIdAndStatusOrderByCreatedAtAsc(UUID equipmentId, WaitlistStatus status);
}