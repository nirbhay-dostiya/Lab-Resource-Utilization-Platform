package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity.SharedEquipmentListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedEquipmentListingRepository extends JpaRepository<SharedEquipmentListing, UUID> {

    // Fetch all equipment listed under a specific sharing agreement
    List<SharedEquipmentListing> findByAgreementId(UUID agreementId);

    // Find the specific listing for a piece of equipment to check if it's currently shared
    Optional<SharedEquipmentListing> findByEquipmentIdAndIsActiveTrue(UUID equipmentId);

    // Fetch all currently active listings (Useful for a global "Marketplace" or catalog view)
    List<SharedEquipmentListing> findByIsActiveTrue();
}