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
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.entity.Notification;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationChannel;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationReferenceType;
import in.sbmtechservice.Lab_Resource_Utilization.notification.enums.NotificationStatus;
import in.sbmtechservice.Lab_Resource_Utilization.notification.repository.NotificationRepository;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository.FundingSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FundingSourceRepository fundingSourceRepository;
    private final ApplicationEventPublisher eventPublisher;

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
                            Notification notif = Notification.builder()
                                    .user(booking.getUser())
                                    .referenceType(NotificationReferenceType.BOOKING)
                                    .referenceId(booking.getId())
                                    .channel(NotificationChannel.IN_APP)
                                    .content("Your purchase is confirmed. Payment of ₹" + request.getAmount() + " for invoice #" + invoice.getId().toString().substring(0,8).toUpperCase() + " has been successfully processed for " + booking.getEquipment().getName() + ". Thank you for booking with LabResource.")
                                    .status(NotificationStatus.SENT)
                                    .isRead(false)
                                    .build();
                            notificationRepository.save(notif);

                            // Notify Seller (Owner Institution Admins)
                            UUID ownerInstId = booking.getEquipment().getDepartment().getInstitution().getId();
                            java.util.List<in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User> instUsers = userRepository.findAllByInstitutionId(ownerInstId);
                            for (in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User u : instUsers) {
                                if (u.getRoles().stream().anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.INSTITUTION_ADMIN || r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.DEPT_HEAD)) {
                                    Notification sellerNotif = Notification.builder()
                                            .user(u)
                                            .referenceType(NotificationReferenceType.BOOKING_APPROVAL_REQUEST)
                                            .referenceId(booking.getId())
                                            .channel(NotificationChannel.IN_APP)
                                            .content("Equipment '" + booking.getEquipment().getName() + "' has been booked by " + booking.getUser().getFirstName() + " " + booking.getUser().getLastName() + ". Payment of ₹" + request.getAmount() + " for invoice #" + invoice.getId().toString().substring(0,8).toUpperCase() + " has been received. Please approve this purchase.")
                                            .status(NotificationStatus.SENT)
                                            .isRead(false)
                                            .build();
                                    notificationRepository.save(sellerNotif);
                                }
                            }
                            
                            // Notify System Admins for conflict resolution & monitoring
                            java.util.List<in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User> sysAdmins = userRepository.findAllByRoleName(in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.SYSTEM_ADMIN);
                            for (in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User admin : sysAdmins) {
                                Notification adminNotif = Notification.builder()
                                        .user(admin)
                                        .referenceType(NotificationReferenceType.BOOKING)
                                        .referenceId(booking.getId())
                                        .channel(NotificationChannel.IN_APP)
                                        .content("SYSTEM ALERT: Invoice #" + invoice.getId().toString().substring(0,8).toUpperCase() + " paid. Amount: ₹" + request.getAmount() + ". Booking: " + booking.getEquipment().getName())
                                        .status(NotificationStatus.SENT)
                                        .isRead(false)
                                        .build();
                                notificationRepository.save(adminNotif);
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

    /**
     * Returns invoices visible to the currently logged-in user's institution.
     * An invoice is visible if:
     *  1. The institution is the PAYER (billed-to institution or billed-to department's institution)
     *  2. The institution is the PROVIDER (owns the equipment in a booking line item)
     *
     * SYSTEM_ADMIN users always see all invoices.
     */
    public List<InvoiceResponse> getInvoicesForMyInstitution(String userEmail, boolean isSystemAdmin) {
        if (isSystemAdmin) {
            return getAllInvoices();
        }
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        if (user.getInstitution() == null) {
            // User is not affiliated with any institution — return empty list
            return java.util.Collections.emptyList();
        }
        return invoiceRepository.findInvoicesByInstitutionInvolved(user.getInstitution().getId())
                .stream()
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

        String approvedByName = invoice.getApprovedBy() != null
                ? invoice.getApprovedBy().getFirstName() + " " + invoice.getApprovedBy().getLastName()
                : null;

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .billedToInstitutionName(invoice.getBilledToInstitution() != null ? invoice.getBilledToInstitution().getName() : null)
                .billedToDepartmentName(invoice.getBilledToDepartment() != null ? invoice.getBilledToDepartment().getName() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .overheadRate(invoice.getOverheadRate())
                .approvedByName(approvedByName)
                .approvedAt(invoice.getApprovedAt())
                .notes(invoice.getNotes())
                .lineItems(itemResponses)
                .transactions(transactionResponses)
                .build();
    }

    // ── Approval Workflow ─────────────────────────────────────────────────────

    /**
     * Transitions a DRAFT invoice to PENDING_APPROVAL.
     */
    @Transactional
    public InvoiceResponse submitForApproval(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be submitted for approval. Current: " + invoice.getStatus());
        }

        invoice.setStatus(in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus.PENDING_APPROVAL);
        Invoice saved = invoiceRepository.save(invoice);

        // Notify institution admins
        if (saved.getBilledToDepartment() != null) {
            var institutionId = saved.getBilledToDepartment().getInstitution().getId();
            eventPublisher.publishEvent(new in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents.InvoiceApprovalRequestedEvent(
                    saved.getId(), institutionId, saved.getTotalAmount().toString()
            ));
        }

        return mapToResponse(saved);
    }

    /**
     * Transitions PENDING_APPROVAL → APPROVED → ISSUED.
     * Applies overhead rate to final totalAmount if applicable.
     */
    @Transactional
    public InvoiceResponse approveInvoice(UUID invoiceId, UUID approverId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only PENDING_APPROVAL invoices can be approved. Current: " + invoice.getStatus());
        }

        // Apply inter-institutional overhead rate if set
        if (invoice.getOverheadRate() != null && invoice.getOverheadRate().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal overhead = invoice.getTotalAmount().multiply(invoice.getOverheadRate());
            invoice.setTotalAmount(invoice.getTotalAmount().add(overhead));
        }

        invoice.setStatus(in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus.ISSUED);
        if (approverId != null) {
            userRepository.findById(approverId).ifPresent(u -> {
                invoice.setApprovedBy(u);
                invoice.setApprovedAt(LocalDateTime.now());
            });
        }

        Invoice saved = invoiceRepository.save(invoice);

        // Notify billed department
        if (saved.getBilledToDepartment() != null) {
            var institutionId = saved.getBilledToDepartment().getInstitution().getId();
            eventPublisher.publishEvent(new in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents.InvoiceApprovedEvent(
                    saved.getId(), saved.getBilledToDepartment().getId(), saved.getTotalAmount().toString()
            ));
        }

        return mapToResponse(saved);
    }

    /**
     * Auto-generate an invoice by aggregating all CONFIRMED bookings
     * for a department within the specified billing period.
     * Applies overhead rate if FundingSource institution differs from host institution.
     */
    @Transactional
    public InvoiceResponse generateAutoInvoice(UUID departmentId,
                                               java.time.LocalDate periodStart,
                                               java.time.LocalDate periodEnd,
                                               UUID fundingSourceId) {
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        // Fetch bookings in period
        var bookings = bookingRepository.findByEquipmentDepartmentId(departmentId);

        Set<InvoiceLineItem> items = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var booking : bookings) {
            if (booking.getStatus() != in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus.CONFIRMED &&
                    booking.getStatus() != in.sbmtechservice.Lab_Resource_Utilization.booking_scheduling.enums.BookingStatus.COMPLETED) {
                continue;
            }
            if (booking.getStartTime().toLocalDate().isBefore(periodStart) ||
                    booking.getEndTime().toLocalDate().isAfter(periodEnd)) {
                continue;
            }

            long hours = java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toHours();
            BigDecimal pricePerHour = booking.getEquipment().getPricePerHour();
            BigDecimal lineTotal = pricePerHour.multiply(BigDecimal.valueOf(Math.max(hours, 1)));

            InvoiceLineItem item = InvoiceLineItem.builder()
                    .referenceType(ReferenceType.BOOKING)
                    .referenceId(booking.getId())
                    .description("Booking: " + booking.getEquipment().getName())
                    .quantity(BigDecimal.valueOf(Math.max(hours, 1)))
                    .unitPrice(pricePerHour)
                    .lineTotal(lineTotal)
                    .build();
            items.add(item);
            total = total.add(lineTotal);
        }

        // Resolve overhead rate from FundingSource
        BigDecimal overheadRate = BigDecimal.ZERO;
        in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource fundingSource = null;
        if (fundingSourceId != null) {
            fundingSource = fundingSourceRepository.findById(fundingSourceId).orElse(null);
            if (fundingSource != null && fundingSource.getInstitutionOrigin() != null) {
                var hostId = department.getInstitution().getId();
                if (!fundingSource.getInstitutionOrigin().getId().equals(hostId)) {
                    // External institution → apply 25% overhead (configurable)
                    overheadRate = new BigDecimal("0.25");
                }
            }
        }

        Invoice invoice = Invoice.builder()
                .billedToDepartment(department)
                .invoiceDate(java.time.LocalDate.now())
                .dueDate(java.time.LocalDate.now().plusDays(30))
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .status(in.sbmtechservice.Lab_Resource_Utilization.cost_billing.enums.InvoiceStatus.DRAFT)
                .totalAmount(total)
                .overheadRate(overheadRate)
                .fundingSource(fundingSource)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        for (InvoiceLineItem item : items) {
            item.setInvoice(saved);
        }
        saved.setLineItems(items);
        invoiceRepository.save(saved);

        return mapToResponse(saved);
    }

    // ── FundingSource CRUD ────────────────────────────────────────────────────

    public in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource saveFundingSource(
            in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource fs) {
        return fundingSourceRepository.save(fs);
    }

    public List<in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource> getAllFundingSources() {
        return fundingSourceRepository.findAll();
    }
}