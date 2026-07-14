package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id", "student_id"}))
@Getter @Setter @NoArgsConstructor
public class EnrollmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id") private ClassroomEntity classroom;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "student_id") private StudentEntity student;
    @Column(name = "enrolled_at", nullable = false) private Instant enrolledAt = Instant.now();
}
