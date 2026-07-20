package in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentRegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UUID institutionId;
}
