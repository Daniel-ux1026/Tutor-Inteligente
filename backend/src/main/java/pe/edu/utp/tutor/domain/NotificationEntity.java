package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "notifications") @Getter @Setter @NoArgsConstructor
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "teacher_id") private TeacherEntity teacher;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "course_id") private CourseEntity course;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "topic_id") private TopicEntity topic;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "attempt_id") private AttemptEntity attempt;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "alert_id") private AlertEntity alert;
    @Column(name = "notification_type", nullable = false, length = 40) private String notificationType;
    @Column(nullable = false, length = 600) private String summary;
    @Column(nullable = false, length = 20) private String priority;
    @Column(name = "context_json", nullable = false, columnDefinition = "nvarchar(max)") private String contextJson;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
