package in.sbmtechservice.Lab_Resource_Utilization.analytics_reporting.entity;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "system_audit_logs",
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
                @Index(name = "idx_audit_created", columnList = "created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // Many-to-One relationship with User (Nullable for system-triggered events)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    // Loose FK to the affected record
    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Maps directly to PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}