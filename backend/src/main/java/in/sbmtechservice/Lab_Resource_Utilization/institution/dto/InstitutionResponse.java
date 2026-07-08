package in.sbmtechservice.Lab_Resource_Utilization.institution.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class InstitutionResponse {
    private UUID id;
    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private boolean isActive;
}