package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "assigned_activities") @Getter @Setter @NoArgsConstructor
public class AssignedActivityEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "teacher_id") private TeacherEntity teacher;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private StudentEntity student;
    @ManyToOne(optional = false) @JoinColumn(name = "topic_id") private TopicEntity topic;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 1500) private String instructions;
    @Column(name = "due_at") private Instant dueAt;
    @Column(nullable = false, length = 20) private String status = "ASSIGNED";
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
