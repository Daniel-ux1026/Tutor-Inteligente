package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "questions") @Getter @Setter @NoArgsConstructor
public class QuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "topic_id") private TopicEntity topic;
    @Column(nullable = false) private int grade;
    @Column(nullable = false, length = 20) private String difficulty;
    @Column(nullable = false, length = 1000) private String prompt;
    @Column(nullable = false, length = 1000) private String explanation;
    @Column(nullable = false) private boolean active = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_teacher_id") private TeacherEntity createdByTeacher;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC") private List<AlternativeEntity> alternatives = new ArrayList<>();
}
