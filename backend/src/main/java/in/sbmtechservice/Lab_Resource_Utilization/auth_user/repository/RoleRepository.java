package in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Fetches the specific role enum (e.g., when assigning a default role to a new user)
    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);
}