package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, UUID> {

    // Fetch a category by its exact name
    Optional<EquipmentCategory> findByName(String name);

    // Validate if a category already exists
    boolean existsByName(String name);
}