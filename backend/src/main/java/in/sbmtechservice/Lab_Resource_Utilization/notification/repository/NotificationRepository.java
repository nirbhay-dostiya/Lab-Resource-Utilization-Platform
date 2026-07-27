package in.sbmtechservice.Lab_Resource_Utilization.notification.repository;

import in.sbmtechservice.Lab_Resource_Utilization.notification.entity.Notification;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Fetch a user's notification inbox (Paginated, as inboxes can get huge)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Fetch only the UNREAD notifications for a user (Useful for the bell icon dropdown)
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    // Quickly count unread notifications to display a badge number on the UI (e.g., 🔴 3)
    long countByUserIdAndIsReadFalse(UUID userId);

    // 🚨 WORKER QUEUE LOGIC: Find notifications that are queued but haven't been sent yet
    List<Notification> findByStatusOrderByCreatedAtAsc(NotificationStatus status);

    // Fetch all notifications related to a specific event (e.g., all alerts sent for Booking ID 123)
    List<Notification> findByReferenceIdOrderByCreatedAtDesc(UUID referenceId);

    // Bulk update: Mark all of a user's notifications as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") UUID userId);
}