package in.sbmtechservice.Lab_Resource_Utilization.booking.entity;

import jakarta.persistence.*;
import in.sbmtechservice.Lab_Resource_Utilization.auth.entity.User;
import in.sbmtechservice.Lab_Resource_Utilization.booking.enums.WaitlistStatus;
import in.sbmtechservice.Lab_Resource_Utilization.inventory.entity.Equipment;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // Many-to-One relationship with Equipment
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // Many-to-One relationship with User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_start", nullable = false)
    private LocalDateTime requestedStart;

    @Column(name = "requested_end", nullable = false)
    private LocalDateTime requestedEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WaitlistStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}