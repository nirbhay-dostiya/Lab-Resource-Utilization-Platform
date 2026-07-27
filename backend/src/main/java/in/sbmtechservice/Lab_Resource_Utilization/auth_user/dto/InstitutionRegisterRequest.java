package in.sbmtechservice.Lab_Resource_Utilization.auth_user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstitutionRegisterRequest {
    // Admin Details
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    // Institution Details
    private String institutionName;
    private String domain;
    private String address;
    private String contactPhone;
}
