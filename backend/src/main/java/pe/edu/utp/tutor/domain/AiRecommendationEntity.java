package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "ai_recommendations") @Getter @Setter @NoArgsConstructor
public class AiRecommendationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false) @JoinColumn(name = "topic_id") private TopicEntity topic;
    @Column(name = "recommended_level", nullable = false, length = 20) private String recommendedLevel;
    @Column(nullable = false, length = 1000) private String explanation;
    @Column(name = "input_json", nullable = false, columnDefinition = "nvarchar(max)") private String inputJson;
    @Column(name = "model_version", nullable = false, length = 80) private String modelVersion;
    @Column(nullable = false, length = 20) private String source;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
