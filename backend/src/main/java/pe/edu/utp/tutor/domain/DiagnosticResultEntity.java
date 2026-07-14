package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "diagnostic_results") @Getter @Setter @NoArgsConstructor
public class DiagnosticResultEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false) @JoinColumn(name = "course_id") private CourseEntity course;
    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2) private BigDecimal scorePercent;
    @Column(name = "initial_level", nullable = false, length = 20) private String initialLevel;
    @Column(name = "answers_json", nullable = false, columnDefinition = "nvarchar(max)") private String answersJson;
    @Column(name = "completed_at", nullable = false) private Instant completedAt = Instant.now();
}
