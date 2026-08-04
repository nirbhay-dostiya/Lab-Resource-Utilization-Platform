package in.sbmtechservice.Lab_Resource_Utilization.notification.service;

import in.sbmtechservice.Lab_Resource_Utilization.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    // Thread-safe map to store open SSE connections per user
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        // Create an emitter with a 30-minute timeout (or no timeout: Long.MAX_VALUE)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE Connection completed for user: {}", userId);
            emitters.remove(userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.info("SSE Connection timed out for user: {}", userId);
            emitter.complete();
            emitters.remove(userId, emitter);
        });
        emitter.onError((e) -> {
            log.error("SSE Connection error for user: {}", userId, e);
            emitter.completeWithError(e);
            emitters.remove(userId, emitter);
        });

        // Send a dummy initialization event to establish the connection immediately
        try {
            emitter.send(SseEmitter.event().name("init").data("Connected to Notification Stream"));
        } catch (IOException e) {
            emitter.completeWithError(e);
            emitters.remove(userId, emitter);
        }

        return emitter;
    }

    public void sendNotification(UUID userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.error("Error sending SSE notification to user: {}", userId, e);
                emitter.completeWithError(e);
                emitters.remove(userId, emitter);
            }
        }
    }
}
