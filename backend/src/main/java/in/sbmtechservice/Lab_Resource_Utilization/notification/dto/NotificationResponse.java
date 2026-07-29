package in.sbmtechservice.Lab_Resource_Utilization.notification.dto;

import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationChannel;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationReferenceType;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String content;
    private NotificationChannel channel;
    private NotificationReferenceType referenceType;
    private UUID referenceId;
    private NotificationStatus status;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    // adding title and message for the frontend
    private String title;
    private String message;
}
