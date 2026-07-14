package pe.edu.utp.tutor.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "alternatives") @Getter @Setter @NoArgsConstructor
public class AlternativeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "question_id") @JsonIgnore private QuestionEntity question;
    @Column(nullable = false, length = 1, columnDefinition = "char(1)") private String label;
    @Column(nullable = false, length = 500) private String text;
    @Column(nullable = false) private boolean correct;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
