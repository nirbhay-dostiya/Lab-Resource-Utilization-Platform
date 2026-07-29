package in.sbmtechservice.Lab_Resource_Utilization.notification.service;

import in.sbmtechservice.Lab_Resource_Utilization.notification.dto.NotificationResponse;
import in.sbmtechservice.Lab_Resource_Utilization.notification.entity.Notification;
import in.sbmtechservice.Lab_Resource_Utilization.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getUnreadNotificationsForUser(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .content(notification.getContent())
                .channel(notification.getChannel())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .status(notification.getStatus())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .title(notification.getReferenceType().name())
                .message(notification.getContent())
                .build();
    }
}
