package in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);
}