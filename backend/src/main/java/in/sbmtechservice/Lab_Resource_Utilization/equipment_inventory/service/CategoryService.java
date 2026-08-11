package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.service;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryRequest;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.dto.CategoryResponse;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.EquipmentCategory;
import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository.EquipmentCategoryRepository;
import in.sbmtechservice.Lab_Resource_Utilization.notification.event.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final EquipmentCategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new equipment category.
     * Fires CategoryAddedEvent so System Admins are notified (category is a global entity).
     * The caller (addedById/addedByName) gets a self-confirmation.
     *
     * @param request    the category data
     * @param addedById  UUID of the user creating this category (may be null for legacy callers)
     * @param addedByName display name of the user creating this category
     */
    public CategoryResponse createCategory(CategoryRequest request,
                                           UUID addedById,
                                           String addedByName) {
        EquipmentCategory category = new EquipmentCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        EquipmentCategory saved = categoryRepository.save(category);

        // Notify System Admins only (global entity — no institution scope)
        if (addedById != null) {
            eventPublisher.publishEvent(new NotificationEvents.CategoryAddedEvent(
                    saved.getId(), saved.getName(), addedById, addedByName
            ));
        }

        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    /**
     * Legacy overload — no notification context available.
     * Kept for backward compatibility with any callers that don't provide user context.
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        return createCategory(request, null, null);
    }

    public java.util.List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .build())
                .toList();
    }
}