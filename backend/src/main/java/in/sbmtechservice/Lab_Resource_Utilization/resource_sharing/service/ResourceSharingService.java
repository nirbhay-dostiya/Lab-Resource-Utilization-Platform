package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.service;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Equipment;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto.AccessRequestDto;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto.SharedEquipmentListingDto;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity.AccessRequest;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity.SharedEquipmentListing;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.entity.SharingAgreement;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.enums.AccessRequestStatus;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.repository.AccessRequestRepository;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.repository.SharedEquipmentListingRepository;
import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.repository.SharingAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceSharingService {

    private final SharedEquipmentListingRepository listingRepository;
    private final SharingAgreementRepository agreementRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;

    @Transactional
    public SharedEquipmentListingDto createListing(SharedEquipmentListingDto dto) {
        SharingAgreement agreement = null;
        if (dto.getAgreementId() != null) {
            agreement = agreementRepository.findById(dto.getAgreementId())
                    .orElseThrow(() -> new RuntimeException("Agreement not found"));
        }
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("Equipment not found"));

        SharedEquipmentListing listing = SharedEquipmentListing.builder()
                .agreement(agreement)
                .equipment(equipment)
                .externalHourlyRate(dto.getExternalHourlyRate() != null ? dto.getExternalHourlyRate() : java.math.BigDecimal.ZERO)
                .termsAndConditions(dto.getTermsAndConditions())
                .availableFrom(dto.getAvailableFrom())
                .availableTo(dto.getAvailableTo())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        SharedEquipmentListing saved = listingRepository.save(listing);
        dto.setId(saved.getId());
        dto.setEquipmentName(equipment.getName());
        return dto;
    }

    public List<SharedEquipmentListingDto> getAllActiveListings() {
        return listingRepository.findAll().stream()
                .filter(SharedEquipmentListing::getIsActive)
                .map(this::mapToListingDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccessRequestDto createAccessRequest(AccessRequestDto dto) {
        SharedEquipmentListing listing = listingRepository.findById(dto.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        User requester = userRepository.findById(dto.getRequesterId())
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        AccessRequest request = AccessRequest.builder()
                .listing(listing)
                .requester(requester)
                .justification(dto.getJustification())
                .requestedStart(dto.getRequestedStart())
                .requestedEnd(dto.getRequestedEnd())
                .status(AccessRequestStatus.PENDING)
                .build();

        AccessRequest saved = accessRequestRepository.save(request);
        dto.setId(saved.getId());
        dto.setStatus(saved.getStatus());
        return dto;
    }

    @Transactional
    public AccessRequestDto updateAccessRequestStatus(UUID requestId, AccessRequestStatus status, UUID approverId) {
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Access Request not found"));
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        request.setStatus(status);
        request.setApprover(approver);
        request.setReviewedAt(LocalDateTime.now());
        
        AccessRequest saved = accessRequestRepository.save(request);
        return mapToAccessRequestDto(saved);
    }
    
    public List<AccessRequestDto> getRequestsByRequester(UUID requesterId) {
         // Placeholder logic. To implement efficiently we would need a method in AccessRequestRepository
         return accessRequestRepository.findAll().stream()
                 .filter(req -> req.getRequester().getId().equals(requesterId))
                 .map(this::mapToAccessRequestDto)
                 .collect(Collectors.toList());
    }

    public List<AccessRequestDto> getRequestsByInstitution(UUID institutionId) {
         return accessRequestRepository.findAll().stream()
                 .filter(req -> req.getListing().getEquipment().getDepartment().getInstitution().getId().equals(institutionId))
                 .map(this::mapToAccessRequestDto)
                 .collect(Collectors.toList());
    }

    private SharedEquipmentListingDto mapToListingDto(SharedEquipmentListing listing) {
        SharedEquipmentListingDto dto = new SharedEquipmentListingDto();
        dto.setId(listing.getId());
        if (listing.getAgreement() != null) {
            dto.setAgreementId(listing.getAgreement().getId());
        }
        dto.setEquipmentId(listing.getEquipment().getId());
        dto.setEquipmentName(listing.getEquipment().getName());
        dto.setInstitutionName(listing.getEquipment().getDepartment().getInstitution().getName());
        dto.setInstitutionId(listing.getEquipment().getDepartment().getInstitution().getId());
        dto.setExternalHourlyRate(listing.getExternalHourlyRate());
        dto.setTermsAndConditions(listing.getTermsAndConditions());
        dto.setAvailableFrom(listing.getAvailableFrom());
        dto.setAvailableTo(listing.getAvailableTo());
        dto.setIsActive(listing.getIsActive());
        
        // Calculate waitlist count
        int waitlistCount = accessRequestRepository.findByListingIdAndStatusOrderByCreatedAtAsc(
                listing.getId(), 
                in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.enums.AccessRequestStatus.PENDING
        ).size();
        dto.setWaitlistCount(waitlistCount);
        
        return dto;
    }

    private AccessRequestDto mapToAccessRequestDto(AccessRequest request) {
        AccessRequestDto dto = new AccessRequestDto();
        dto.setId(request.getId());
        dto.setListingId(request.getListing().getId());
        dto.setRequesterId(request.getRequester().getId());
        dto.setJustification(request.getJustification());
        dto.setRequestedStart(request.getRequestedStart());
        dto.setRequestedEnd(request.getRequestedEnd());
        dto.setStatus(request.getStatus());
        dto.setEquipmentName(request.getListing().getEquipment().getName());
        dto.setInstitutionName(request.getListing().getEquipment().getDepartment().getInstitution().getName());
        dto.setRequesterName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName());
        if (request.getRequester().getInstitution() != null) {
            dto.setRequesterInstitutionName(request.getRequester().getInstitution().getName());
        } else if (request.getRequester().getDepartment() != null && request.getRequester().getDepartment().getInstitution() != null) {
            dto.setRequesterInstitutionName(request.getRequester().getDepartment().getInstitution().getName());
        } else {
            boolean isSysAdmin = request.getRequester().getRoles().stream()
                    .anyMatch(r -> r.getName() == in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType.SYSTEM_ADMIN);
            dto.setRequesterInstitutionName(isSysAdmin ? "System Admin (Global)" : "No Institution Assigned");
        }
        return dto;
    }
}
