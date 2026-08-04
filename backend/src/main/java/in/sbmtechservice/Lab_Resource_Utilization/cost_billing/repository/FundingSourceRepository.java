package in.sbmtechservice.Lab_Resource_Utilization.cost_billing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.cost_billing.entity.FundingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FundingSourceRepository extends JpaRepository<FundingSource, UUID> {

    List<FundingSource> findByInstitutionOriginId(UUID institutionId);

    List<FundingSource> findByExpirationDateBefore(java.time.LocalDate date);
}
