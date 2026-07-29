package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.controller;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.dto.BudgetResponse;
import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetRepository budgetRepository;

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_HEAD') or hasAuthority('INSTITUTION_ADMIN')")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByDepartment(@PathVariable UUID departmentId) {
        List<BudgetResponse> budgets = budgetRepository.findByDepartmentIdOrderByFiscalYearDesc(departmentId).stream()
                .map(b -> BudgetResponse.builder()
                        .id(b.getId())
                        .departmentId(b.getDepartment().getId())
                        .departmentName(b.getDepartment().getName())
                        .fiscalYear(b.getFiscalYear())
                        .allocatedAmount(b.getAllocatedAmount())
                        .spentAmount(b.getSpentAmount())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(budgets);
    }
}
