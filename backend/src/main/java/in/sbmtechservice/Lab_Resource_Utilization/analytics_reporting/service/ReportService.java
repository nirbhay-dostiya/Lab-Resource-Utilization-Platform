package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.maintenance_calibration.repository.MaintenanceTaskRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OEE (Overall Equipment Effectiveness) Report Service.
 *
 * OEE = Availability × Performance × Quality (each expressed 0.0–1.0)
 *
 * Availability = (scheduled_hours − downtime_hours) / scheduled_hours
 * Performance  = actual_usage_hours / scheduled_hours
 * Quality      = confirmed_bookings / total_bookings
 *
 * Idle cost = downtime_hours × price_per_hour
 *
 * Report generation is @Async — does not block the HTTP thread.
 * Results are stored in an in-memory map keyed by reportId (UUID).
 * In production, replace with Redis or a DB-backed store.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceTaskRepository maintenanceTaskRepository;

    /** In-memory report store. Replace with Redis in prod. */
    private final ConcurrentHashMap<UUID, ReportResult> reportStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> reportStatus = new ConcurrentHashMap<>(); // PENDING / DONE / FAILED

    // ── Report Result DTO ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class ReportResult {
        private UUID reportId;
        private LocalDate from;
        private LocalDate to;
        private List<EquipmentOeeEntry> entries;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    public static class EquipmentOeeEntry {
        private String equipmentId;
        private String equipmentName;
        private double scheduledHours;
        private double usageHours;
        private double downtimeHours;
        private long totalBookings;
        private long confirmedBookings;
        private double availability;
        private double performance;
        private double quality;
        private double oeeScore;
        private BigDecimal idleCost;
        private BigDecimal pricePerHour;
    }

    // ── Async Report Generation ───────────────────────────────────────────────

    /**
     * Kicks off async OEE report generation.
     * Returns a reportId immediately; the caller polls /reports/{reportId}/status.
     */
    public UUID initiateReport(List<UUID> equipmentIds, LocalDate from, LocalDate to) {
        UUID reportId = UUID.randomUUID();
        reportStatus.put(reportId, "PENDING");
        generateAsync(reportId, equipmentIds, from, to);
        return reportId;
    }

    @Async
    public void generateAsync(UUID reportId, List<UUID> equipmentIds, LocalDate from, LocalDate to) {
        try {
            log.info("[REPORT] Starting OEE report {} for {} equipment ({} → {})",
                    reportId, equipmentIds.size(), from, to);

            List<Equipment> equipmentList = equipmentIds.isEmpty()
                    ? equipmentRepository.findAll()
                    : equipmentRepository.findAllById(equipmentIds);

            LocalDateTime fromDt = from.atStartOfDay();
            LocalDateTime toDt = to.atTime(23, 59, 59);
            long periodHours = ChronoUnit.HOURS.between(fromDt, toDt);
            double scheduledHours = Math.max(periodHours, 1);

            List<EquipmentOeeEntry> entries = new ArrayList<>();

            for (Equipment eq : equipmentList) {
                // All bookings in period
                var bookings = bookingRepository.findByEquipmentIdsAndDateRange(
                        List.of(eq.getId()), fromDt, toDt);

                long total = bookings.size();
                long confirmed = bookings.stream()
                        .filter(b -> b.getStatus().name().equals("CONFIRMED") ||
                                b.getStatus().name().equals("COMPLETED") ||
                                b.getStatus().name().equals("IN_USE"))
                        .count();

                // Actual usage hours (sum of confirmed booking durations)
                double usageHours = bookings.stream()
                        .filter(b -> b.getStatus().name().equals("CONFIRMED") ||
                                b.getStatus().name().equals("COMPLETED") ||
                                b.getStatus().name().equals("IN_USE"))
                        .mapToLong(b -> ChronoUnit.HOURS.between(b.getStartTime(), b.getEndTime()))
                        .sum();

                // Downtime from maintenance tasks in period
                double downtimeHours = maintenanceTaskRepository
                        .findByEquipmentIdOrderByScheduledDateDesc(eq.getId()).stream()
                        .filter(t -> t.getScheduledDate() != null &&
                                !t.getScheduledDate().isBefore(fromDt) &&
                                !t.getScheduledDate().isAfter(toDt))
                        .mapToDouble(t -> t.getDowntimeHours() != null ?
                                t.getDowntimeHours().doubleValue() : 0.0)
                        .sum();

                double availability = Math.min(1.0,
                        (scheduledHours - downtimeHours) / scheduledHours);
                double performance = Math.min(1.0, usageHours / scheduledHours);
                double quality = total == 0 ? 1.0 : (double) confirmed / total;
                double oee = availability * performance * quality;

                BigDecimal pricePerHour = eq.getPricePerHour() != null ?
                        eq.getPricePerHour() : BigDecimal.ZERO;
                BigDecimal idleCost = pricePerHour.multiply(
                        BigDecimal.valueOf(downtimeHours).setScale(2, RoundingMode.HALF_UP));

                entries.add(EquipmentOeeEntry.builder()
                        .equipmentId(eq.getId().toString())
                        .equipmentName(eq.getName())
                        .scheduledHours(scheduledHours)
                        .usageHours(usageHours)
                        .downtimeHours(downtimeHours)
                        .totalBookings(total)
                        .confirmedBookings(confirmed)
                        .availability(round2(availability))
                        .performance(round2(performance))
                        .quality(round2(quality))
                        .oeeScore(round2(oee))
                        .idleCost(idleCost)
                        .pricePerHour(pricePerHour)
                        .build());
            }

            ReportResult result = ReportResult.builder()
                    .reportId(reportId)
                    .from(from)
                    .to(to)
                    .entries(entries)
                    .generatedAt(LocalDateTime.now())
                    .build();

            reportStore.put(reportId, result);
            reportStatus.put(reportId, "DONE");
            log.info("[REPORT] Completed report {} — {} entries", reportId, entries.size());

        } catch (Exception e) {
            log.error("[REPORT] Failed report {}: {}", reportId, e.getMessage(), e);
            reportStatus.put(reportId, "FAILED");
        }
    }

    // ── Status & Retrieval ────────────────────────────────────────────────────

    public String getStatus(UUID reportId) {
        return reportStatus.getOrDefault(reportId, "NOT_FOUND");
    }

    public Optional<ReportResult> getResult(UUID reportId) {
        return Optional.ofNullable(reportStore.get(reportId));
    }

    // ── Export: CSV ───────────────────────────────────────────────────────────

    public byte[] exportToCsv(ReportResult report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Equipment Name,Scheduled Hours,Usage Hours,Downtime Hours,")
                .append("Total Bookings,Confirmed Bookings,Availability,Performance,Quality,OEE Score,")
                .append("Price/Hr (₹),Idle Cost (₹)\n");

        for (var entry : report.getEntries()) {
            sb.append(csvEscape(entry.getEquipmentName())).append(",")
                    .append(entry.getScheduledHours()).append(",")
                    .append(entry.getUsageHours()).append(",")
                    .append(entry.getDowntimeHours()).append(",")
                    .append(entry.getTotalBookings()).append(",")
                    .append(entry.getConfirmedBookings()).append(",")
                    .append(pct(entry.getAvailability())).append(",")
                    .append(pct(entry.getPerformance())).append(",")
                    .append(pct(entry.getQuality())).append(",")
                    .append(pct(entry.getOeeScore())).append(",")
                    .append(entry.getPricePerHour()).append(",")
                    .append(entry.getIdleCost()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ── Export: PDF (OpenPDF / iText) ─────────────────────────────────────────

    public byte[] exportToPdf(ReportResult report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document doc = new Document(PageSize.A4.rotate()); // Landscape for wide table
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

            Paragraph title = new Paragraph("OEE Equipment Effectiveness Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph("Period: " + report.getFrom() + " → " + report.getTo()));
            doc.add(new Paragraph("Generated: " + report.getGeneratedAt()));
            doc.add(Chunk.NEWLINE);

            // Table
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(12);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            String[] headers = {"Equipment", "Sched.Hr", "Usage Hr", "Down Hr",
                    "Total Bk", "Conf. Bk", "Avail%", "Perf%", "Qual%", "OEE%",
                    "₹/Hr", "Idle ₹"};

            for (String h : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(63, 84, 186));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                table.addCell(cell);
            }

            for (var entry : report.getEntries()) {
                Color rowColor = entry.getOeeScore() >= 0.65 ? new Color(232, 255, 234) :
                        entry.getOeeScore() >= 0.40 ? new Color(255, 251, 224) :
                                new Color(255, 234, 234);
                addPdfRow(table, bodyFont, rowColor,
                        entry.getEquipmentName(),
                        fmt(entry.getScheduledHours()),
                        fmt(entry.getUsageHours()),
                        fmt(entry.getDowntimeHours()),
                        String.valueOf(entry.getTotalBookings()),
                        String.valueOf(entry.getConfirmedBookings()),
                        pct(entry.getAvailability()),
                        pct(entry.getPerformance()),
                        pct(entry.getQuality()),
                        pct(entry.getOeeScore()),
                        entry.getPricePerHour().toPlainString(),
                        entry.getIdleCost().toPlainString()
                );
            }

            doc.add(table);
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("OEE = Availability × Performance × Quality. " +
                    "Green ≥ 65% | Yellow 40–64% | Red < 40%", bodyFont));

            doc.close();
        } catch (Exception e) {
            log.error("[REPORT] PDF generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
        return baos.toByteArray();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void addPdfRow(com.lowagie.text.pdf.PdfPTable table, Font font, Color bg, String... values) {
        for (String v : values) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(v != null ? v : "", font));
            cell.setBackgroundColor(bg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }
    }

    private String pct(double val) { return String.format("%.1f%%", val * 100); }
    private String fmt(double val) { return String.format("%.1f", val); }
    private double round2(double val) { return Math.round(val * 100.0) / 100.0; }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
