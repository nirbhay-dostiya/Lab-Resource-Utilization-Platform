package in.sbmtechservice.Lab_Resource_Utilization.notification.repository;

import in.sbmtechservice.Lab_Resource_Utilization.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    // Fetch a template by its unique business code (e.g., "BOOKING_CONFIRMED")
    Optional<NotificationTemplate> findByCode(String code);

    // Validate if a template code already exists during admin setup
    boolean existsByCode(String code);
}