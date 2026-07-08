package in.sbmtechservice.Lab_Resource_Utilization.institution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"institution_id", "code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // Owning side of the Many-to-One relationship with Institution
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Inverse side of the One-to-Many relationship with the Associative Entity (UserDepartment)
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserDepartment> userMemberships = new HashSet<>();
}
