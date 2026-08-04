package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.controller;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.InvoiceRequest;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.InvoiceResponse;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.TransactionRequest;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('FINANCE_ADMIN')")
    public ResponseEntity<String> createInvoice(@RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(billingService.createInvoice(request));
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> payInvoice(@PathVariable UUID id, @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(billingService.payInvoice(id, request));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getInvoiceById(id));
    }

    @GetMapping("/invoices/department/{departmentId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('INSTITUTION_USER')")
    public ResponseEntity<java.util.List<InvoiceResponse>> getInvoicesByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(billingService.getInvoicesByDepartment(departmentId));
    }

    /**
     * Returns invoices that involve the current user's institution —
     * either as the PAYER or as the PROVIDER of equipment.
     * SYSTEM_ADMIN sees all invoices.
     */
    @GetMapping("/invoices/my-institution")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<InvoiceResponse>> getMyInstitutionInvoices(Principal principal) {
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SYSTEM_ADMIN"));
        return ResponseEntity.ok(billingService.getInvoicesForMyInstitution(principal.getName(), isAdmin));
    }

    /**
     * Admin-only: returns ALL invoices in the system.
     */
    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<java.util.List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(billingService.getAllInvoices());
    }

    // ── Approval Workflow ──────────────────────────────────────────────────────

    /**
     * Submit a DRAFT invoice for manager approval.
     * Transitions: DRAFT → PENDING_APPROVAL
     */
    @PostMapping("/invoices/{id}/submit-for-approval")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> submitForApproval(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.submitForApproval(id));
    }

    /**
     * Approve a PENDING_APPROVAL invoice.
     * Transitions: PENDING_APPROVAL → APPROVED → ISSUED
     * Role gate: LAB_MANAGER, DEPT_HEAD, INSTITUTION_ADMIN, SYSTEM_ADMIN
     */
    @PostMapping("/invoices/{id}/approve")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<InvoiceResponse> approveInvoice(
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        UUID approverId = null;
        if (userDetails instanceof in.sbmtechservice.Lab_Resource_Utilization.auth_user.security.CustomUserDetails cud) {
            approverId = cud.getId();
        }
        return ResponseEntity.ok(billingService.approveInvoice(id, approverId));
    }

    /**
     * Auto-generate an invoice from all confirmed bookings in a billing period.
     * Applies inter-institutional overhead rate if fundingSource institution differs.
     */
    @PostMapping("/invoices/generate-auto")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<InvoiceResponse> generateAutoInvoice(
            @RequestParam UUID departmentId,
            @RequestParam String periodStart,
            @RequestParam String periodEnd,
            @RequestParam(required = false) UUID fundingSourceId) {
        return ResponseEntity.ok(billingService.generateAutoInvoice(
                departmentId,
                java.time.LocalDate.parse(periodStart),
                java.time.LocalDate.parse(periodEnd),
                fundingSourceId
        ));
    }

    // ── Funding Sources ────────────────────────────────────────────────────────

    @PostMapping("/funding-sources")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('INSTITUTION_ADMIN') or hasAuthority('LAB_MANAGER')")
    public ResponseEntity<in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource> createFundingSource(
            @RequestBody in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource fs) {
        return ResponseEntity.ok(billingService.saveFundingSource(fs));
    }

    @GetMapping("/funding-sources")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource>> getAllFundingSources() {
        return ResponseEntity.ok(billingService.getAllFundingSources());
    }
}