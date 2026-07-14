package pe.edu.utp.tutor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "teachers") @Getter @Setter @NoArgsConstructor
public class TeacherEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "user_id", unique = true) private UserEntity user;
    @Column(length = 120) private String specialty;
}
