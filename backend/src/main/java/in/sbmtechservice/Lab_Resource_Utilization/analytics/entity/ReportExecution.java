package in.sbmtechservice.Lab_Resource_Utilization.analytics.entity;

import in.sbmtechservice.Lab_Resource_Utilization.analytics.enums.ExecutionStatus;
import in.sbmtechservice.Lab_Resource_Utilization.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReportExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // The saved configuration template used
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saved_report_id", nullable = false)
    private SavedReport savedReport;

    // The user who triggered the run
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executed_by", nullable = false)
    private User executedBy;

    @CreationTimestamp
    @Column(name = "execution_time", nullable = false, updatable = false)
    private LocalDateTime executionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExecutionStatus status;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;
}