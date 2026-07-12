package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.enums.EquipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class EquipmentResponse {
    private UUID id;
    private String name;
    private String manufacturer;

    private String modelNumber;
    private String serialNumber;
    private String description;
    private EquipmentStatus status;
    private String documentationUrl;

    private UUID departmentId;
    private String departmentName;


    private UUID categoryId;
    private String categoryName;

    private Set<String> tags;
    private UUID institutionId;
    private String institutionName;
}