package in.sbmtechservice.Lab_Resource_Utilization.institution.repository;

import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    // Useful for validating unique domains during registration/tenant setup
    boolean existsByDomain(String domain);

    // Fetch an institution by its unique domain
    Optional<Institution> findByDomain(String domain);

    // Check if an institution name is already taken
    boolean existsByName(String name);
}