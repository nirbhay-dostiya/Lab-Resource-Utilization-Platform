package in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import lombok.Data;
import java.util.UUID;

@Data
public class RoleAssignmentRequest {
    private UUID userId;
    private RoleType newRole;
    private UUID institutionId;
}