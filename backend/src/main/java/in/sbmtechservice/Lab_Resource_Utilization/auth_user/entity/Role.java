package in.sbmtechservice.Lab_Resource_Utilization.auth_user.entity;

import in.sbmtechservice.Lab_Resource_Utilization.auth_user.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleType name;

    @Column(length = 255)
    private String description;

    // Inverse side of the Many-to-Many relationship with User
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private Set<User> users = new HashSet<>();

    // Owning side of the Many-to-Many relationship with Permission
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
