package in.sbmtechservice.Lab_Resource_Utilization.institution.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AssignDepartmentRequest {
    private UUID userId;
    private UUID departmentId;
}