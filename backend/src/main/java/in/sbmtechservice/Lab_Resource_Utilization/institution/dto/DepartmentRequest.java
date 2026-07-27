package in.sbmtechservice.Lab_Resource_Utilization.institution.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DepartmentRequest {
    private String name;
    private String description;
    private UUID institutionId;
    private String code;
    private String domain;
    private String address;

}