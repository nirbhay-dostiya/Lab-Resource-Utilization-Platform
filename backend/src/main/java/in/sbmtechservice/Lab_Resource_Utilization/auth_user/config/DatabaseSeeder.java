package in.sbmtechservice.Lab_Resource_Utilization.auth_user.config;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.Role;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.RoleRepository;
import in.sbmtechservice.Lab_Resource_Utilization.auth_user.repository.UserRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Department;
import in.sbmtechservice.Lab_Resource_Utilization.institution.entity.Institution;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.DepartmentRepository;
import in.sbmtechservice.Lab_Resource_Utilization.institution.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking database for default roles...");
        try {
            jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
            log.info("Successfully dropped roles_name_check constraint if it existed.");
        } catch (Exception e) {
            log.warn("Could not drop roles_name_check constraint. It may not exist. Error: {}", e.getMessage());
        }

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

        seedUsers();
    }

    private void seedUsers() {
        log.info("Checking database for default admin users...");

        // 1. Seed System Admin
        if (!userRepository.existsByEmail("nirbhay@gmail.com")) {
            Role sysAdminRole = roleRepository.findByName(RoleType.SYSTEM_ADMIN).orElseThrow();
            User sysAdmin = User.builder()
                    .firstName("Nirbhay")
                    .lastName("Admin")
                    .email("nirbhay@gmail.com")
                    .passwordHash(passwordEncoder.encode("Strong12345"))
                    .isActive(true)
                    .build();
            sysAdmin.getRoles().add(sysAdminRole);
            userRepository.save(sysAdmin);
            log.info("Inserted default System Admin user (nirbhay@gmail.com).");
        }

        if (!userRepository.existsByEmail("systemadmin@gmail.com")) {
            Role sysAdminRole = roleRepository.findByName(RoleType.SYSTEM_ADMIN).orElseThrow();
            User sysAdmin2 = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email("systemadmin@gmail.com")
                    .passwordHash(passwordEncoder.encode("Strong12345"))
                    .isActive(true)
                    .build();
            sysAdmin2.getRoles().add(sysAdminRole);
            userRepository.save(sysAdmin2);
            log.info("Inserted second System Admin user (systemadmin@gmail.com).");
        }

        // 2. Seed Institution Admin
        if (!userRepository.existsByEmail("instadmin@example.com")) {
            
            // Ensure there is at least one Institution
            Institution defaultInst;
            if (institutionRepository.count() == 0) {
                defaultInst = Institution.builder()
                        .name("Default Institution")
                        .domain("example.com")
                        .contactEmail("contact@example.com")
                        .contactPhone("1234567890")
                        .address("123 Default St")
                        .isActive(true)
                        .build();
                defaultInst = institutionRepository.save(defaultInst);
                log.info("Inserted Default Institution");
            } else {
                defaultInst = institutionRepository.findAll().get(0);
            }

            // Ensure there is at least one Department for the institution
            Department defaultDept;
            if (departmentRepository.count() == 0) {
                defaultDept = Department.builder()
                        .name("Main Department")
                        .code("MAIN-01")
                        .description("Default Department")
                        .institution(defaultInst)
                        .isActive(true)
                        .build();
                defaultDept = departmentRepository.save(defaultDept);
                log.info("Inserted Default Department");
            } else {
                defaultDept = departmentRepository.findAll().get(0);
            }

            Role instAdminRole = roleRepository.findByName(RoleType.INSTITUTION_ADMIN).orElseThrow();
            User instAdmin = User.builder()
                    .firstName("Institution")
                    .lastName("Admin")
                    .email("instadmin@example.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .department(defaultDept)
                    .institution(defaultInst)
                    .isActive(true)
                    .build();
            instAdmin.getRoles().add(instAdminRole);
            userRepository.save(instAdmin);
            log.info("Inserted default Institution Admin user (instadmin@example.com).");
        }

        log.info("Admin user seeding completed successfully.");
    }
}