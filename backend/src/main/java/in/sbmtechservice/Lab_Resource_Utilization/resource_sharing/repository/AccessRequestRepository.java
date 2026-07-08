package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.repository;

import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity.AccessRequest;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.enums.AccessRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, UUID> {

    // Fetch all requests made by a specific guest researcher
    List<AccessRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    // Fetch all requests targeting a specific shared listing (Useful for the Host's approval dashboard)
    List<AccessRequest> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    // Fetch pending requests for a specific listing
    List<AccessRequest> findByListingIdAndStatusOrderByCreatedAtAsc(UUID listingId, AccessRequestStatus status);

    // Fetch all requests approved or rejected by a specific host manager (Audit trail)
    List<AccessRequest> findByApproverIdOrderByReviewedAtDesc(UUID approverId);
}