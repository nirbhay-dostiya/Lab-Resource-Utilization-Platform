package in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.repository;

import in.sbmtechservice.Lab_Resource_Utilization.equipment_inventory.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    // Fetch a tag by its exact name
    Optional<Tag> findByName(String name);

    // Validate if a tag already exists
    boolean existsByName(String name);
}