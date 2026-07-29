package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.service;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.InvoiceRequest;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.InvoiceResponse;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.TransactionRequest;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.TransactionResponse;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.Invoice;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.InvoiceLineItem;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.Transaction;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.ReferenceType;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.TransactionStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.entity.Booking;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus;
import in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.repository.BookingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository.BudgetRepository;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository.InvoiceRepository;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository.TransactionRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final DepartmentRepository departmentRepository;
    private final BudgetRepository budgetRepository;
    private final BookingRepository bookingRepository;
    // private final InstitutionRepository institutionRepository; // Uncomment if you have this!

    @Transactional
    public String createInvoice(InvoiceRequest request) {
        // 1. Enforce the XOR Rule before Hibernate does
        if ((request.getBilledToInstitutionId() == null && request.getBilledToDepartmentId() == null) ||
                (request.getBilledToInstitutionId() != null && request.getBilledToDepartmentId() != null)) {
            throw new IllegalArgumentException("Invoice must be billed to EXACTLY ONE target (Institution OR Department).");
        }

        Invoice invoice = Invoice.builder()
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .status(InvoiceStatus.ISSUED)
                .build();

        // 2. Assign Target (Assumes you have repositories for these!)
        if (request.getBilledToDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getBilledToDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            invoice.setBilledToDepartment(dept);
        } else {
            // Institution inst = institutionRepository.findById(request.getBilledToInstitutionId()).orElseThrow();
            // invoice.setBilledToInstitution(inst);
        }

        // 3. Process Line Items & Calculate Total
        Set<InvoiceLineItem> items = new HashSet<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (InvoiceRequest.LineItemRequest itemReq : request.getLineItems()) {
            InvoiceLineItem item = new InvoiceLineItem();
            item.setInvoice(invoice); // Set parent
            item.setReferenceType(itemReq.getReferenceType());
            item.setReferenceId(itemReq.getReferenceId());
            item.setDescription(itemReq.getDescription());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());

            // Manual calculation for grand total (Entity PrePersist handles its own line total)
            BigDecimal lineTotal = itemReq.getQuantity().multiply(itemReq.getUnitPrice());
            grandTotal = grandTotal.add(lineTotal);

            items.add(item);
        }

        invoice.setLineItems(items);
        invoice.setTotalAmount(grandTotal);

        Invoice saved = invoiceRepository.save(invoice);
        return "Invoice Created with ID: " + saved.getId() + " | Total: $" + grandTotal;
    }

    @Transactional
    public String payInvoice(UUID invoiceId, TransactionRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found."));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already paid.");
        }

        // 1. Create Transaction
        Transaction transaction = new Transaction();
        transaction.setInvoice(invoice);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setReferenceNumber(request.getReferenceNumber());
        TransactionStatus currentStatus = request.getStatus() != null ? request.getStatus() : TransactionStatus.SUCCESS;
        transaction.setStatus(currentStatus);
        transactionRepository.save(transaction);

        if (currentStatus == TransactionStatus.FAILED) {
            return "Payment failed. Please try again.";
        }

        // 2. Check if Fully Paid
        BigDecimal totalPaid = invoice.getTransactions().stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(request.getAmount()); // Add current payment

        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);

            // 3. CORE LOGIC: If this is an internal department, deduct from their budget!
            if (invoice.getBilledToDepartment() != null) {
                int currentYear = LocalDateTime.now().getYear();
                budgetRepository.findByDepartmentIdAndFiscalYear(invoice.getBilledToDepartment().getId(), currentYear)
                        .ifPresent(budget -> {
                            budget.setSpentAmount(budget.getSpentAmount().add(invoice.getTotalAmount()));
                            budgetRepository.save(budget);
                        });
            }

            // 4. If this invoice is for a booking, update the booking status to PENDING
            invoice.getLineItems().stream()
                    .filter(item -> item.getReferenceType() == ReferenceType.BOOKING && item.getReferenceId() != null)
                    .forEach(item -> {
                        bookingRepository.findById(item.getReferenceId()).ifPresent(booking -> {
                            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                                booking.setStatus(BookingStatus.PENDING);
                                bookingRepository.save(booking);
                            }
                        });
                    });
        }

        invoiceRepository.save(invoice);
        return "Payment processed successfully. Current Status: " + invoice.getStatus();
    }

    public InvoiceResponse getInvoiceById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found."));

        return mapToResponse(invoice);
    }

    public List<InvoiceResponse> getInvoicesByDepartment(UUID departmentId) {
        return invoiceRepository.findByBilledToDepartmentIdOrderByInvoiceDateDesc(departmentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceResponse.LineItemResponse> itemResponses = invoice.getLineItems().stream()
                .map(item -> {
                    InvoiceResponse.LineItemResponse.LineItemResponseBuilder builder = InvoiceResponse.LineItemResponse.builder()
                            .id(item.getId())
                            .referenceType(item.getReferenceType())
                            .description(item.getDescription())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .referenceId(item.getReferenceId());
                            
                    if (item.getReferenceType() == ReferenceType.BOOKING && item.getReferenceId() != null) {
                        bookingRepository.findById(item.getReferenceId()).ifPresent(booking -> {
                            if (booking.getEquipment() != null) {
                                builder.equipmentName(booking.getEquipment().getName());
                                if (booking.getEquipment().getDepartment() != null && booking.getEquipment().getDepartment().getInstitution() != null) {
                                    builder.equipmentInstituteName(booking.getEquipment().getDepartment().getInstitution().getName());
                                }
                            }
                            builder.bookingStartTime(booking.getStartTime());
                            builder.bookingEndTime(booking.getEndTime());
                        });
                    }
                    
                    return builder.build();
                })
                .toList();

        List<TransactionResponse> transactionResponses = invoice.getTransactions() != null ? 
                invoice.getTransactions().stream()
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .transactionDate(t.getTransactionDate())
                        .amount(t.getAmount())
                        .paymentMethod(t.getPaymentMethod())
                        .referenceNumber(t.getReferenceNumber())
                        .status(t.getStatus())
                        .build())
                .toList() : java.util.Collections.emptyList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .billedToInstitutionName(invoice.getBilledToInstitution() != null ? invoice.getBilledToInstitution().getName() : null)
                .billedToDepartmentName(invoice.getBilledToDepartment() != null ? invoice.getBilledToDepartment().getName() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .lineItems(itemResponses)
                .transactions(transactionResponses)
                .build();
    }
}