package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "alerts") @Getter @Setter @NoArgsConstructor
public class AlertEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "course_id") private CourseEntity course;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "topic_id") private TopicEntity topic;
    @Column(name = "alert_type", nullable = false, length = 40) private String alertType;
    @Column(nullable = false, length = 600) private String reason;
    @Column(name = "risk_level", nullable = false, length = 20) private String riskLevel;
    @Column(name = "pedagogical_recommendation", nullable = false, length = 1000) private String pedagogicalRecommendation;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "resolved_at") private Instant resolvedAt;
}
