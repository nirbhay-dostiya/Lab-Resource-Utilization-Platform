package in.sbmtechservice.Lab_Resource_Utilization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LabResourceUtilizationApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabResourceUtilizationApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner dropNotNullConstraint(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE shared_equipment_listings ALTER COLUMN agreement_id DROP NOT NULL");
				System.out.println("Successfully dropped NOT NULL constraint on agreement_id");
			} catch (Exception e) {
				System.err.println("Could not drop NOT NULL constraint: " + e.getMessage());
			}
		};
	}

}
