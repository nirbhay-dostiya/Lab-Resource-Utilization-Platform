package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class EquipmentRequest {
    private String name;
    private String serialNumber;
    private String description;
    private UUID departmentId;
    private UUID categoryId;
    private String manufacturer;
    private String modelNumber;
    private String documentationUrl;
    private String imageBase64;
    private Set<UUID> tagIds;
}