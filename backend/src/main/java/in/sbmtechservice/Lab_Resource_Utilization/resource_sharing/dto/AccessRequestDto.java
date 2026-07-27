package in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.dto;

import in.sbmtechservice.Lab_Resource_Utilization.resource_sharing.enums.AccessRequestStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class AccessRequestDto {
    private UUID id;
    private UUID listingId;
    private UUID requesterId;
    private String justification;
    private java.time.LocalDate requestedStart;
    private java.time.LocalDate requestedEnd;
    private AccessRequestStatus status;
    private String equipmentName;
    private String institutionName;
    private String requesterName;
    private String requesterInstitutionName;
}
