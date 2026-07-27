package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String description;
}