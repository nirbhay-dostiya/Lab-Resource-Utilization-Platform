package in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Crucial for Spring Security's UserDetailsService implementation
    Optional<User> findByEmail(String email);

    // Highly optimized check for registration validation
    boolean existsByEmail(String email);

    // Example of a custom JPQL query to fetch a user with roles explicitly initialized
    // (useful to avoid LazyInitializationException during authentication)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    java.util.List<User> findAllByRoleName(@Param("roleName") in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType roleName);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles LEFT JOIN u.department d LEFT JOIN u.institution i WHERE (i.id = :institutionId OR d.institution.id = :institutionId)")
    java.util.List<User> findAllByInstitutionId(@Param("institutionId") UUID institutionId);
}