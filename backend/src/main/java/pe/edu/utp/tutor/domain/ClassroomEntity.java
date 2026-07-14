package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "classrooms") @Getter @Setter @NoArgsConstructor
public class ClassroomEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "teacher_id") private TeacherEntity teacher;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private int grade;
    @Column(nullable = false, length = 10) private String section;
    @Column(name = "inactivity_days", nullable = false) private int inactivityDays = 7;
}
