package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    // Fetch all budgets for a specific department across different years
    List<Budget> findByDepartmentIdOrderByFiscalYearDesc(UUID departmentId);

    // Fetch the specific budget for a department for the current fiscal year
    Optional<Budget> findByDepartmentIdAndFiscalYear(UUID departmentId, Integer fiscalYear);
}