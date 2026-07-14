package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "teacher_observations") @Getter @Setter @NoArgsConstructor
public class TeacherObservationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "teacher_id") private TeacherEntity teacher;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private StudentEntity student;
    @Column(nullable = false, length = 2000) private String text;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
