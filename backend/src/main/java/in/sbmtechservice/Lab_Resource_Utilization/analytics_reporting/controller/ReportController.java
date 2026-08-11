package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.controller;

import in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.service.ReportService;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OEE Report Controller.
 *
 * Usage flow:
 *  1. POST /api/reports/generate → returns { "reportId": "..." }
 *  2. GET  /api/reports/{reportId}/status → returns "PENDING" | "DONE" | "FAILED"
 *  3. GET  /api/reports/{reportId}/download?format=csv|pdf → download file
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    /**
     * Initiate async OEE report generation.
     * Returns immediately with a reportId for polling.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<Map<String, String>> generateReport(
            @RequestParam(required = false) List<UUID> equipmentIds,
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Resolve the requesting user's ID so the notification fires back only to them
        UUID requesterId = null;
        if (userDetails != null) {
            requesterId = userRepository.findByEmail(userDetails.getUsername())
                    .map(u -> u.getId()).orElse(null);
        }

        List<UUID> ids = equipmentIds != null ? equipmentIds : List.of();
        UUID reportId = reportService.initiateReport(ids, LocalDate.parse(from), LocalDate.parse(to), requesterId);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "reportId", reportId.toString(),
                        "status", "PENDING",
                        "message", "Report generation started. Poll /api/reports/" + reportId + "/status"
                ));
    }

    /**
     * Check report generation status.
     * Returns: PENDING | DONE | FAILED | NOT_FOUND
     */
    @GetMapping("/{reportId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable UUID reportId) {
        String status = reportService.getStatus(reportId);
        return ResponseEntity.ok(Map.of("reportId", reportId.toString(), "status", status));
    }

    /**
     * Download the completed report as CSV or PDF.
     * Only available when status == DONE.
     */
    @GetMapping("/{reportId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable UUID reportId,
            @RequestParam(defaultValue = "csv") String format) {

        var result = reportService.getResult(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not ready or not found: " + reportId));

        HttpHeaders headers = new HttpHeaders();
        String filename = "oee-report-" + result.getFrom() + "-to-" + result.getTo();

        byte[] data;
        if ("pdf".equalsIgnoreCase(format)) {
            data = reportService.exportToPdf(result);
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename + ".pdf");
        } else {
            data = reportService.exportToCsv(result);
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename + ".csv");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * Retrieve the completed report result as JSON.
     * Use this to display results in the UI after status == DONE.
     * Does NOT trigger a new report generation.
     */
    @GetMapping("/{reportId}/result")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReportResult(@PathVariable UUID reportId) {
        return reportService.getResult(reportId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convenience endpoint: generate + get inline JSON result (synchronous, small datasets only).
     */
    @GetMapping("/inline")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<?> getInlineReport(
            @RequestParam(required = false) List<UUID> equipmentIds,
            @RequestParam String from,
            @RequestParam String to) {

        List<UUID> ids = equipmentIds != null ? equipmentIds : List.of();
        UUID rid = reportService.initiateReport(ids, LocalDate.parse(from), LocalDate.parse(to));

        // Wait up to 10 seconds for the async job to finish (for small datasets)
        for (int i = 0; i < 10; i++) {
            try { Thread.sleep(1000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            String s = reportService.getStatus(rid);
            if ("DONE".equals(s)) return ResponseEntity.ok(reportService.getResult(rid).orElseThrow());
            if ("FAILED".equals(s)) return ResponseEntity.internalServerError().body(Map.of("error", "Report generation failed"));
        }
        return ResponseEntity.accepted().body(Map.of("reportId", rid.toString(), "status", "PENDING"));
    }
}
