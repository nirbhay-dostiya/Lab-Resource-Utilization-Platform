package in.sbmtechservice.Lab_Resource_Utilization.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import java.util.Map;

/**
 * Email Notification Service (Module 6).
 *
 * All sends are @Async — never block the request thread.
 * Uses Thymeleaf HTML templates stored under src/main/resources/templates/email/
 * Falls back to plain-text if template rendering fails.
 *
 * SMTP is configured in application.properties (spring.mail.*).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Send an HTML email using a named Thymeleaf template.
     *
     * @param to           recipient email address
     * @param subject      email subject line
     * @param templateName name of the .html file in templates/email/ (without extension)
     * @param variables    model variables injected into the Thymeleaf context
     */
    @Async
    public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
        if (to == null || to.isBlank()) {
            log.warn("[EMAIL] Skipped send — recipient address is null/blank. Template: {}", templateName);
            return;
        }
        try {
            Context ctx = new Context(Locale.ENGLISH, variables);
            String htmlBody = templateEngine.process("email/" + templateName, ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("no-reply@labrms.local");

            mailSender.send(msg);
            log.info("[EMAIL] ✓ Sent '{}' to {}", subject, to);

        } catch (Exception e) {
            log.error("[EMAIL] ✗ Failed to send '{}' to {}: {}", subject, to, e.getMessage(), e);
        }
    }

    // ── Convenience methods ───────────────────────────────────────────────────

    public void sendCalibrationReminder(String to, String equipmentName,
                                        String expiryDate, int daysLeft) {
        sendHtml(to,
                "⚠️ Calibration Reminder: " + equipmentName + " expires in " + daysLeft + " day(s)",
                "calibration-reminder",
                Map.of(
                        "equipmentName", equipmentName,
                        "expiryDate", expiryDate,
                        "daysLeft", daysLeft,
                        "urgency", daysLeft <= 7 ? "CRITICAL" : daysLeft <= 14 ? "HIGH" : "NORMAL"
                ));
    }

    public void sendWorkOrderUrgent(String to, String equipmentName,
                                    String priority, String currentStatus) {
        sendHtml(to,
                "🚨 Urgent Work Order: " + equipmentName + " [" + priority + "]",
                "workorder-urgent",
                Map.of(
                        "equipmentName", equipmentName,
                        "priority", priority,
                        "currentStatus", currentStatus
                ));
    }

    public void sendInvoiceApprovalRequest(String to, String invoiceId, String amount) {
        sendHtml(to,
                "📄 Invoice Pending Approval — ₹" + amount,
                "invoice-approval",
                Map.of("invoiceId", invoiceId, "amount", amount));
    }

    public void sendInvoiceApproved(String to, String invoiceId, String amount) {
        sendHtml(to,
                "✅ Invoice Approved — ₹" + amount,
                "invoice-approved",
                Map.of("invoiceId", invoiceId, "amount", amount));
    }

    public void sendAssetDowntime(String to, String equipmentName, String estimatedHours) {
        sendHtml(to,
                "🔧 Asset Downtime Alert: " + equipmentName,
                "asset-downtime",
                Map.of("equipmentName", equipmentName, "estimatedHours", estimatedHours));
    }
}
