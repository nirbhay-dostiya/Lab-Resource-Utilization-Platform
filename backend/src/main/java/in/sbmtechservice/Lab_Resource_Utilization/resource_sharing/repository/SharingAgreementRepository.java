package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity.SharingAgreement;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.enums.AgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharingAgreementRepository extends JpaRepository<SharingAgreement, UUID> {

    // Find all agreements where an institution is the HOST (providing equipment)
    List<SharingAgreement> findByHostInstitutionIdOrderByStartDateDesc(UUID hostInstitutionId);

    // Find all agreements where an institution is the GUEST (consuming equipment)
    List<SharingAgreement> findByGuestInstitutionIdOrderByStartDateDesc(UUID guestInstitutionId);

    // Find agreements by their current status
    List<SharingAgreement> findByStatus(AgreementStatus status);

    // Check if an active agreement already exists between two specific institutions
    @Query("SELECT sa FROM SharingAgreement sa WHERE sa.hostInstitution.id = :hostId " +
            "AND sa.guestInstitution.id = :guestId AND sa.status = 'ACTIVE'")
    Optional<SharingAgreement> findActiveAgreementBetween(
            @Param("hostId") UUID hostId,
            @Param("guestId") UUID guestId
    );

    // Find agreements that are expiring soon (to trigger renewal notifications)
    List<SharingAgreement> findByStatusAndEndDateBetween(
            AgreementStatus status,
            LocalDate startDate,
            LocalDate endDate
    );
}