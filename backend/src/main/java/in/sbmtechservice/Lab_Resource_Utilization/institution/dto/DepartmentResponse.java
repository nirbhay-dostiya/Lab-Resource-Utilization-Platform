package in.sbmtechservice.Lab_Resource_Utilization.institution.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class DepartmentResponse {
    private UUID id;
    private String name;
    private String description;
    private String code;
    private UUID institutionId;
    private String institutionName;
}