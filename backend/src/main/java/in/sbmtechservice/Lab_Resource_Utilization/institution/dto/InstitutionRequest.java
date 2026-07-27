package in.sbmtechservice.Lab_Resource_Utilization.institution.dto;

import lombok.Data;

@Data
public class InstitutionRequest {
    private String name;
    private String address;
    private String domain;
    private String contactEmail;
    private String contactPhone;
}
