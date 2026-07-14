package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "attempts") @Getter @Setter @NoArgsConstructor
public class AttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "client_attempt_id", nullable = false, unique = true) private UUID clientAttemptId;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "question_id") private QuestionEntity question;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "selected_alternative_id") private AlternativeEntity selectedAlternative;
    @Column(nullable = false) private boolean correct;
    @Column(name = "response_time_seconds", nullable = false) private int responseTimeSeconds;
    @Column(name = "level_before", nullable = false, length = 20) private String levelBefore;
    @Column(name = "recommended_level", nullable = false, length = 20) private String recommendedLevel;
    @Column(name = "ai_explanation", nullable = false, length = 1000) private String aiExplanation;
    @Column(name = "recommendation_source", nullable = false, length = 20) private String recommendationSource;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "received_at", nullable = false) private Instant receivedAt = Instant.now();
}
