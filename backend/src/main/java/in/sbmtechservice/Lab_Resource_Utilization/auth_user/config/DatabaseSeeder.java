package in.sbmtechservice.Lab_Resource_Utilization.auth_user.config;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking database for default roles...");

        // Iterate through all the roles defined in your RoleType Enum
        for (RoleType roleType : RoleType.values()) {
            // If the role doesn't exist in the DB, create it!
            if (!roleRepository.existsByName(roleType)) {
                Role newRole = Role.builder()
                        .name(roleType)
                        .description("Default system role for " + roleType.name())
                        .build();

                roleRepository.save(newRole);
                log.info("Inserted new role into database: {}", roleType.name());
            }
        }

        log.info("Role seeding completed successfully.");
    }
}