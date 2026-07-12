package in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private List<String> roles;
}
