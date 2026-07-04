package in.sbmtechservice.Lab_Resource_Utilization.institution.entity;

import in.sbmtechservice.Lab_Resource_Utilization.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDepartment {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private UserDepartmentId id = new UserDepartmentId();

    // Maps the user_id from the embedded composite key to the User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    // Maps the department_id from the embedded composite key to the Department entity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("departmentId")
    @JoinColumn(name = "department_id")
    private Department department;

    // The "Payload" column that forces us to make this explicit class
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    // Architect Note: Helper constructor to easily link User and Department in your service layer
    public UserDepartment(User user, Department department, Boolean isPrimary) {
        this.user = user;
        this.department = department;
        this.isPrimary = isPrimary;
        this.id = new UserDepartmentId(user.getId(), department.getId());
    }
}