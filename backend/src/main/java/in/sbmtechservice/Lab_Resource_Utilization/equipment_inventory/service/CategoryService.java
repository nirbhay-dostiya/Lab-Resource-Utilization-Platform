package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryRequest;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.EquipmentCategory;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final EquipmentCategoryRepository categoryRepository;

    public CategoryResponse createCategory(CategoryRequest request) {
        EquipmentCategory category = new EquipmentCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        EquipmentCategory saved = categoryRepository.save(category);

        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }
}