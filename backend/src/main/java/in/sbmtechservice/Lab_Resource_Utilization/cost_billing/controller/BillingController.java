package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.controller;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.InvoiceRequest;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.InvoiceResponse;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.TransactionRequest;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD')")
    public ResponseEntity<String> payInvoice(@PathVariable UUID id, @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(billingService.payInvoice(id, request));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getInvoiceById(id));
    }
}