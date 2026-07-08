package in.sbmtechservice.Lab_Resource_Utilization.notification.repository;

import in.sbmtechservice.Lab_Resource_Utilization.notification.entity.UserNotificationPreference;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UUID> {

    // Fetch all notification preferences for a specific user to display on their settings page
    List<UserNotificationPreference> findByUserId(UUID userId);

    // Check a user's specific preference for a channel before sending an alert
    Optional<UserNotificationPreference> findByUserIdAndChannel(UUID userId, NotificationChannel channel);
}