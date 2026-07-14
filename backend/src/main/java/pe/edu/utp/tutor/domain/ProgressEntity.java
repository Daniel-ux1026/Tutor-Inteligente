package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "progress", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "topic_id"}))
@Getter @Setter @NoArgsConstructor
public class ProgressEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "topic_id") private TopicEntity topic;
    @Column(name = "current_level", nullable = false, length = 20) private String currentLevel = "basico";
    @Column(name = "recommended_level", nullable = false, length = 20) private String recommendedLevel = "basico";
    @Column(name = "total_attempts", nullable = false) private int totalAttempts;
    @Column(name = "correct_attempts", nullable = false) private int correctAttempts;
    @Column(name = "average_time_seconds", nullable = false, precision = 10, scale = 2) private BigDecimal averageTimeSeconds = BigDecimal.ZERO;
    @Column(name = "success_streak", nullable = false) private int successStreak;
    @Column(name = "consecutive_errors", nullable = false) private int consecutiveErrors;
    @Column(name = "completed_activities", nullable = false) private int completedActivities;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
}
